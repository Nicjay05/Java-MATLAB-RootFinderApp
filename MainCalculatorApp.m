classdef MainCalculatorApp < matlab.apps.AppBase

    %% PROPERTIES
    properties (Access = public)

        UIFigure            matlab.ui.Figure
        TabGroup            matlab.ui.container.TabGroup

        % ROOT TAB
        RootTab             matlab.ui.container.Tab
        EquationDropDown    matlab.ui.control.DropDown
        FunctionField       matlab.ui.control.EditField
        MethodDropDown      matlab.ui.control.DropDown
        % Parameters
        XlField             matlab.ui.control.EditField
        XuField             matlab.ui.control.EditField
        X0Field             matlab.ui.control.EditField
        IterField           matlab.ui.control.EditField
        SolveButton         matlab.ui.control.Button
        ExitButton          matlab.ui.control.Button
        UITable             matlab.ui.control.Table
        UIAxes              matlab.ui.control.UIAxes

        % MATRIX TAB
        MatrixTab               matlab.ui.container.Tab
        MatrixAField            matlab.ui.control.TextArea
        MatrixBField            matlab.ui.control.TextArea
        MatrixOperationDropDown matlab.ui.control.DropDown
        MatrixSolveButton       matlab.ui.control.Button
        MatrixClearButton       matlab.ui.control.Button
        MatrixResultArea        matlab.ui.control.TextArea
    end

    %% COLOR PALETTE — Navy Blue & Khaki
    properties (Access = private)
        BG_DARK     = [0.05 0.08 0.16]
        BG_PANEL    = [0.08 0.12 0.22]
        BG_INPUT    = [0.10 0.15 0.28]
        ACCENT      = [0.76 0.70 0.50]
        ACCENT2     = [0.87 0.82 0.62]
        ACCENT_DARK = [0.35 0.30 0.18]
        TXT_PRIMARY = [0.93 0.91 0.82]
        TXT_MUTED   = [0.60 0.62 0.55]
        TXT_DARK    = [0.05 0.06 0.10]
        CLR_DANGER  = [0.72 0.22 0.22]
        CLR_NEUTRAL = [0.14 0.19 0.32]
    end

    %% EQUATION PRESETS
    properties (Access = private)
        EquationPresets = { ...
            '-- Select Preset --',        '', ...
            'Cubic',                      'x^3 - x - 2', ...
            'Quadratic',                  'x^2 - 4', ...
            'Simple Polynomial',          'x^3 - 2*x^2 + x - 1', ...
            'Exponential',                'exp(x) - 3*x', ...
            'Mixed Polynomial',           'x^4 - 3*x^3 + x^2 - 2*x + 1', ...
            'High Degree Polynomial',     'x^6 - 7*x^5 + x^4 - 3*x^2 + 2', ...
            'Newton-Raphson (classic)',   'x^3 - 2*x - 5', ...
            'Trigonometric',              'sin(x) - x/2', ...
            'Logarithmic',               'log(x) - x + 2', ...
        }
    end

    %% PRIVATE METHODS
    methods (Access = private)

        %% EQUATION PRESET CHANGED
        function onEquationPresetChanged(app, ~)
            sel = app.EquationDropDown.Value;
            presets = app.EquationPresets;
            for k = 1:2:length(presets)-1
                if strcmp(presets{k}, sel) && ~isempty(presets{k+1})
                    app.FunctionField.Value = presets{k+1};
                    return;
                end
            end
        end

        %% ROOT SOLVER
        function solveRoot(app, ~)
            try
                funcStr = strtrim(app.FunctionField.Value);
                if isempty(funcStr)
                    uialert(app.UIFigure, 'Please enter a function f(x).', 'Input Required');
                    return;
                end
                f = str2func(['@(x)' funcStr]);
                method = app.MethodDropDown.Value;

                % Read parameters from UI fields
                xL   = str2double(app.XlField.Value);
                xU   = str2double(app.XuField.Value);
                x0   = str2double(app.X0Field.Value);
                maxI = round(str2double(app.IterField.Value));
                tol  = 0.001;

                % Validate
                if any(isnan([xL xU x0 maxI]))
                    uialert(app.UIFigure, 'Please enter valid numbers for all parameters.', 'Invalid Input');
                    return;
                end
                if maxI < 1; maxI = 100; end

                rows     = {};
                colNames = {};
                x1 = xU;   % second point for Secant

                switch method
                    case 'Incremental'
                        colNames = {'i','xL','xU','f(xL)','f(xU)'};
                        step = (xU - xL) / maxI;
                        i = 1;
                        cur = xL;
                        while cur < xU && i <= maxI
                            nxt = cur + step;
                            rows{end+1} = {i, cur, nxt, f(cur), f(nxt)}; %#ok<AGROW>
                            if f(cur)*f(nxt) < 0; break; end
                            cur = nxt; i = i + 1;
                        end

                    case 'Bisection'
                        colNames = {'i','xL','xU','xR','Ea(%)'};
                        xr_old = 0; i = 1;
                        while i <= maxI
                            xR = (xL + xU)/2;
                            Ea = 100;
                            if i > 1; Ea = abs((xR - xr_old)/xR)*100; end
                            rows{end+1} = {i, xL, xU, xR, Ea}; %#ok<AGROW>
                            if f(xL)*f(xR) < 0; xU = xR; else; xL = xR; end
                            if Ea < tol; break; end
                            xr_old = xR; i = i + 1;
                        end

                    case 'False Position'
                        colNames = {'i','xL','xU','xR','Ea(%)'};
                        xr_old = 0; i = 1;
                        while i <= maxI
                            denom = f(xL) - f(xU);
                            if abs(denom) < 1e-12; break; end
                            xR = xU - (f(xU)*(xL - xU))/denom;
                            Ea = 100;
                            if i > 1; Ea = abs((xR - xr_old)/xR)*100; end
                            rows{end+1} = {i, xL, xU, xR, Ea}; %#ok<AGROW>
                            if f(xL)*f(xR) < 0; xU = xR; else; xL = xR; end
                            if Ea < tol; break; end
                            xr_old = xR; i = i + 1;
                        end

                    case 'Newton-Raphson'
                        colNames = {'i','x','f(x)','Ea(%)'};
                        df = @(x) (f(x+1e-6)-f(x))/1e-6;
                        cur = x0; i = 1;
                        while i <= maxI
                            dfval = df(cur);
                            if abs(dfval) < 1e-12
                                uialert(app.UIFigure, 'Derivative near zero — Newton-Raphson failed.', 'Error');
                                return;
                            end
                            xnew = cur - f(cur)/dfval;
                            Ea = abs((xnew - cur)/xnew)*100;
                            rows{end+1} = {i, xnew, f(xnew), Ea}; %#ok<AGROW>
                            if Ea < tol; break; end
                            cur = xnew; i = i + 1;
                        end

                    case 'Secant'
                        colNames = {'i','x','Ea(%)'};
                        p0 = x0; p1 = x1; i = 1;
                        while i <= maxI
                            denom = f(p1) - f(p0);
                            if abs(denom) < 1e-12; break; end
                            p2 = p1 - f(p1)*(p1 - p0)/denom;
                            Ea = abs((p2 - p1)/p2)*100;
                            rows{end+1} = {i, p2, Ea}; %#ok<AGROW>
                            if Ea < tol; break; end
                            p0 = p1; p1 = p2; i = i + 1;
                        end
                end

                % Build table
                nCols = length(colNames);
                nRows = length(rows);
                tableData = cell(nRows, nCols);
                for r = 1:nRows
                    for c = 1:nCols
                        tableData{r,c} = rows{r}{c};
                    end
                end
                app.UITable.Data        = tableData;
                app.UITable.ColumnName  = colNames;
                app.UITable.ColumnWidth = repmat({'auto'}, 1, nCols);

                % Plot
                xPlot = linspace(xL - abs(xU-xL), xU + abs(xU-xL), 400);
                y = arrayfun(f, xPlot);
                cla(app.UIAxes);
                hold(app.UIAxes, 'on');
                patch(app.UIAxes, [xPlot fliplr(xPlot)], [y zeros(1,length(y))], ...
                    app.ACCENT_DARK, 'FaceAlpha', 0.3, 'EdgeColor', 'none');
                plot(app.UIAxes, xPlot, y, 'Color', app.ACCENT, 'LineWidth', 2.2);
                yline(app.UIAxes, 0, 'Color', app.ACCENT2, ...
                    'LineWidth', 1.4, 'LineStyle', '--');

                % Mark root(s) with a bright gold dot
                for k = 1:length(xPlot)-1
                    if y(k)*y(k+1) < 0
                        rootX = xPlot(k) - y(k)*(xPlot(k+1)-xPlot(k))/(y(k+1)-y(k));
                        plot(app.UIAxes, rootX, 0, 'o', ...
                            'MarkerSize', 10, ...
                            'MarkerFaceColor', [1.0 0.86 0.31], ...
                            'MarkerEdgeColor', app.BG_DARK, ...
                            'LineWidth', 1.5);
                    end
                end

                hold(app.UIAxes, 'off');
                grid(app.UIAxes, 'on');

            catch ME
                uialert(app.UIFigure, ME.message, 'Error', 'Icon', 'error');
            end
        end

        %% MATRIX SOLVER
        function solveMatrix(app, ~)
            try
                A = str2num(app.MatrixAField.Value{1}); %#ok<ST2NM>
                if isempty(strtrim(app.MatrixBField.Value{1}))
                    B = [];
                else
                    B = str2num(app.MatrixBField.Value{1}); %#ok<ST2NM>
                end
                operation = app.MatrixOperationDropDown.Value;
                switch operation
                    case 'Matrix Addition';       result = A + B;
                    case 'Matrix Multiplication'; result = A * B;
                    case 'Adjoint';               result = det(A) * inv(A);
                    case 'Inverse Matrix';        result = inv(A);
                    case 'Determinant';           result = det(A);
                    case 'Power of Matrix';       result = A^2;
                    case 'Solve Equations';       result = A\B;
                    case 'Transpose';             result = A';
                end
                app.MatrixResultArea.Value = evalc('disp(result)');
            catch ME
                app.MatrixResultArea.Value = {['Error: ' ME.message]};
            end
        end

        %% CLEAR MATRIX
        function clearMatrix(app, ~)
            app.MatrixAField.Value     = {''};
            app.MatrixBField.Value     = {''};
            app.MatrixResultArea.Value = {''};
        end

        %% EXIT
        function exitProgram(app, ~)
            delete(app.UIFigure);
        end

        %% HELPER — styled label
        function lbl = mkLabel(~, parent, txt, pos, sz, bold, col)
            lbl = uilabel(parent, ...
                'Text',            txt, ...
                'Position',        pos, ...
                'FontSize',        sz, ...
                'FontWeight',      bold, ...
                'FontColor',       col, ...
                'BackgroundColor', 'none');
        end

        %% HELPER — styled edit field
        function ef = mkEdit(app, parent, val, pos)
            ef = uieditfield(parent, 'text', ...
                'Position',        pos, ...
                'Value',           val, ...
                'FontSize',        11, ...
                'FontColor',       app.TXT_PRIMARY, ...
                'BackgroundColor', app.BG_INPUT);
        end

        %% HELPER — styled panel
        function p = mkPanel(app, parent, pos, title, titleColor)
            p = uipanel(parent, ...
                'Position',        pos, ...
                'BackgroundColor', app.BG_PANEL, ...
                'ForegroundColor', titleColor, ...
                'Title',           title, ...
                'FontSize',        10, ...
                'FontWeight',      'bold', ...
                'BorderType',      'line');
        end
    end

    %% CONSTRUCTOR
    methods (Access = public)
        function app = MainCalculatorApp

            W = 980; H = 680;

            %% ── MAIN FIGURE ─────────────────────────────────────────────
            app.UIFigure = uifigure( ...
                'Position', [80 40 W H], ...
                'Name',     'Numerical Methods & Matrix Calculator', ...
                'Resize',   'off');
            app.UIFigure.Color = app.BG_DARK;

            %% ── TAB GROUP ───────────────────────────────────────────────
            app.TabGroup = uitabgroup(app.UIFigure, 'Position', [0 0 W H]);

            %% ════════════════════════════════════════════════════════════
            %%  ROOT FINDING TAB
            %% ════════════════════════════════════════════════════════════
            app.RootTab = uitab(app.TabGroup, 'Title', '  Root Finding  ');
            app.RootTab.BackgroundColor = app.BG_DARK;

            % ── Header bar ──────────────────────────────────────────────
            hdr = uipanel(app.RootTab, ...
                'Position',        [8 632 964 36], ...
                'BackgroundColor', app.BG_PANEL, ...
                'BorderType',      'line', ...
                'ForegroundColor', app.ACCENT_DARK, ...
                'Title',           '');
            app.mkLabel(hdr, 'ROOT FINDER',                    [12 7 140 20], 13, 'bold',   app.ACCENT);
            app.mkLabel(hdr, '— Numerical Methods Calculator', [152 7 300 20], 11, 'normal', app.TXT_MUTED);

            % ── Equation bar (preset + f(x) field) ──────────────────────
            eqPanel = uipanel(app.RootTab, ...
                'Position',        [8 568 964 58], ...
                'BackgroundColor', app.BG_PANEL, ...
                'BorderType',      'line', ...
                'ForegroundColor', app.ACCENT_DARK, ...
                'Title',           '');

            app.mkLabel(eqPanel, 'Preset:', [12 30 52 18], 9, 'bold', app.TXT_MUTED);

            presets = app.EquationPresets;
            presetNames = {};
            for k = 1:2:length(presets)
                presetNames{end+1} = presets{k}; %#ok<AGROW>
            end
            app.EquationDropDown = uidropdown(eqPanel, ...
                'Position',        [68 28 240 20], ...
                'Items',           presetNames, ...
                'Value',           '-- Select Preset --', ...
                'FontSize',        9, ...
                'FontColor',       app.TXT_PRIMARY, ...
                'BackgroundColor', app.BG_INPUT, ...
                'ValueChangedFcn', @(~,~) onEquationPresetChanged(app));

            app.mkLabel(eqPanel, 'f(x) =', [320 28 48 20], 11, 'bold', app.ACCENT2);
            app.FunctionField = uieditfield(eqPanel, 'text', ...
                'Position',        [370 26 280 22], ...
                'Value',           'x^3 - x - 2', ...
                'FontSize',        11, ...
                'FontColor',       app.TXT_PRIMARY, ...
                'BackgroundColor', app.BG_INPUT, ...
                'Tooltip',         'Type any function of x');

            app.mkLabel(eqPanel, 'Method:', [660 28 56 20], 10, 'normal', app.TXT_MUTED);
            app.MethodDropDown = uidropdown(eqPanel, ...
                'Position',        [718 26 170 22], ...
                'Items',           {'Incremental','Bisection','False Position','Newton-Raphson','Secant'}, ...
                'FontSize',        10, ...
                'FontColor',       app.TXT_PRIMARY, ...
                'BackgroundColor', app.BG_INPUT);

            app.mkLabel(eqPanel, '(or type below)', [316 10 160 16], 8, 'normal', app.TXT_MUTED);

            % ── Parameters bar ───────────────────────────────────────────
            prmPanel = uipanel(app.RootTab, ...
                'Position',        [8 514 964 48], ...
                'BackgroundColor', app.BG_PANEL, ...
                'BorderType',      'line', ...
                'ForegroundColor', app.ACCENT_DARK, ...
                'Title',           '');

            % Xl
            app.mkLabel(prmPanel, 'Xl:', [10 14 24 20], 11, 'bold', app.ACCENT);
            app.XlField = app.mkEdit(prmPanel, '0', [36 13 70 22]);

            % Xu
            app.mkLabel(prmPanel, 'Xu:', [118 14 24 20], 11, 'bold', app.ACCENT);
            app.XuField = app.mkEdit(prmPanel, '3', [144 13 70 22]);

            % x0
            app.mkLabel(prmPanel, 'x\x2080:', [226 14 30 20], 11, 'bold', app.ACCENT);
            app.X0Field = app.mkEdit(prmPanel, '1', [258 13 70 22]);

            % Iterations
            app.mkLabel(prmPanel, 'Iterations:', [340 14 72 20], 11, 'bold', app.ACCENT);
            app.IterField = app.mkEdit(prmPanel, '10', [416 13 60 22]);

            % Solve & Exit buttons
            app.SolveButton = uibutton(prmPanel, ...
                'Text',            'Solve', ...
                'Position',        [500 10 110 28], ...
                'FontSize',        11, ...
                'FontWeight',      'bold', ...
                'FontColor',       app.TXT_DARK, ...
                'BackgroundColor', app.ACCENT, ...
                'ButtonPushedFcn', @(~,~) solveRoot(app));

            app.ExitButton = uibutton(prmPanel, ...
                'Text',            'Exit', ...
                'Position',        [622 10 90 28], ...
                'FontSize',        11, ...
                'FontWeight',      'bold', ...
                'FontColor',       app.TXT_PRIMARY, ...
                'BackgroundColor', app.CLR_DANGER, ...
                'ButtonPushedFcn', @(~,~) exitProgram(app));

            % ── Table ───────────────────────────────────────────────────
            app.mkLabel(app.RootTab, 'Iteration Results', [10 504 200 16], 9, 'bold', app.ACCENT2);

            app.UITable = uitable(app.RootTab, ...
                'Position',        [8 48 408 462], ...
                'FontSize',        10, ...
                'BackgroundColor', app.BG_INPUT, ...
                'RowStriping',     'off');

            % ── Plot panel ──────────────────────────────────────────────
            pltPanel = app.mkPanel(app.RootTab, [424 48 548 462], ' Function Plot ', app.ACCENT2);

            app.UIAxes = uiaxes(pltPanel, ...
                'Position',  [8 8 530 438], ...
                'Color',     app.BG_INPUT, ...
                'XColor',    app.TXT_MUTED, ...
                'YColor',    app.TXT_MUTED, ...
                'GridColor', [0.18 0.22 0.38], ...
                'GridAlpha', 0.9, ...
                'XGrid',     'on', ...
                'YGrid',     'on', ...
                'Box',       'on');
            app.UIAxes.Title.Color  = app.TXT_PRIMARY;
            app.UIAxes.Title.String = 'f(x)';

            %% ════════════════════════════════════════════════════════════
            %%  MATRIX TAB
            %% ════════════════════════════════════════════════════════════
            app.MatrixTab = uitab(app.TabGroup, 'Title', '  Matrix Operations  ');
            app.MatrixTab.BackgroundColor = app.BG_DARK;

            mhdr = uipanel(app.MatrixTab, ...
                'Position',        [8 632 964 36], ...
                'BackgroundColor', app.BG_PANEL, ...
                'BorderType',      'line', ...
                'ForegroundColor', app.ACCENT_DARK, ...
                'Title',           '');
            app.mkLabel(mhdr, 'MATRIX OPERATIONS',      [12 7 190 20], 13, 'bold',   app.ACCENT);
            app.mkLabel(mhdr, '— Linear Algebra Solver', [202 7 240 20], 11, 'normal', app.TXT_MUTED);

            aPanel = app.mkPanel(app.MatrixTab, [8 370 296 256], ' Matrix A ', app.ACCENT);
            app.MatrixAField = uitextarea(aPanel, ...
                'Position',        [6 6 282 228], ...
                'Value',           {'[1 2; 3 4]'}, ...
                'FontSize',        12, ...
                'FontColor',       app.TXT_PRIMARY, ...
                'BackgroundColor', app.BG_INPUT);

            bPanel = app.mkPanel(app.MatrixTab, [312 370 296 256], ' Matrix B ', app.ACCENT);
            app.MatrixBField = uitextarea(bPanel, ...
                'Position',        [6 6 282 228], ...
                'Value',           {'[5 6; 7 8]'}, ...
                'FontSize',        12, ...
                'FontColor',       app.TXT_PRIMARY, ...
                'BackgroundColor', app.BG_INPUT);

            ctrlPanel = app.mkPanel(app.MatrixTab, [616 370 356 256], ' Controls ', app.ACCENT2);

            app.mkLabel(ctrlPanel, 'Operation', [10 210 100 18], 10, 'bold', app.TXT_MUTED);
            app.MatrixOperationDropDown = uidropdown(ctrlPanel, ...
                'Position',        [10 180 332 26], ...
                'Items',           {'Matrix Addition','Matrix Multiplication','Adjoint', ...
                                    'Inverse Matrix','Determinant','Power of Matrix', ...
                                    'Solve Equations','Transpose'}, ...
                'FontSize',        11, ...
                'FontColor',       app.TXT_PRIMARY, ...
                'BackgroundColor', app.BG_INPUT);

            app.mkLabel(ctrlPanel, ...
                'Tip: Leave Matrix B empty for single-matrix ops.', ...
                [10 145 332 30], 9, 'normal', app.TXT_MUTED);

            app.MatrixSolveButton = uibutton(ctrlPanel, ...
                'Text',            'Compute', ...
                'Position',        [10 92 332 36], ...
                'FontSize',        12, ...
                'FontWeight',      'bold', ...
                'FontColor',       app.TXT_DARK, ...
                'BackgroundColor', app.ACCENT, ...
                'ButtonPushedFcn', @(~,~) solveMatrix(app));

            app.MatrixClearButton = uibutton(ctrlPanel, ...
                'Text',            'Clear All', ...
                'Position',        [10 46 332 36], ...
                'FontSize',        12, ...
                'FontWeight',      'bold', ...
                'FontColor',       app.TXT_PRIMARY, ...
                'BackgroundColor', app.CLR_NEUTRAL, ...
                'ButtonPushedFcn', @(~,~) clearMatrix(app));

            resPanel = app.mkPanel(app.MatrixTab, [8 48 964 316], ' Result ', app.ACCENT2);
            app.MatrixResultArea = uitextarea(resPanel, ...
                'Position',        [6 6 950 290], ...
                'FontSize',        13, ...
                'FontColor',       app.ACCENT2, ...
                'BackgroundColor', app.BG_INPUT);
        end
    end
end