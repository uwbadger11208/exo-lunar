function varargout = PointingViewer(varargin)
% POINTINGVIEWER MATLAB code for PointingViewer.fig
%      POINTINGVIEWER, by itself, creates a new POINTINGVIEWER or raises the existing
%      singleton*.
%
%      H = POINTINGVIEWER returns the handle to a new POINTINGVIEWER or the handle to
%      the existing singleton*.
%
%      POINTINGVIEWER('CALLBACK',hObject,eventData,handles,...) calls the local
%      function named CALLBACK in POINTINGVIEWER.M with the given input arguments.
%
%      POINTINGVIEWER('Property','Value',...) creates a new POINTINGVIEWER or raises the
%      existing singleton*.  Starting from the left, property value pairs are
%      applied to the GUI before PointingViewer_OpeningFcn gets called.  An
%      unrecognized property name or invalid value makes property application
%      stop.  All inputs are passed to PointingViewer_OpeningFcn via varargin.
%
%      *See GUI Options on GUIDE's Tools menu.  Choose "GUI allows only one
%      instance to run (singleton)".
%
% See also: GUIDE, GUIDATA, GUIHANDLES

% Edit the above text to modify the response to help PointingViewer

% Last Modified by GUIDE v2.5 18-May-2016 16:59:05

% Begin initialization code - DO NOT EDIT
gui_Singleton = 1;
gui_State = struct('gui_Name',       mfilename, ...
    'gui_Singleton',  gui_Singleton, ...
    'gui_OpeningFcn', @PointingViewer_OpeningFcn, ...
    'gui_OutputFcn',  @PointingViewer_OutputFcn, ...
    'gui_LayoutFcn',  [] , ...
    'gui_Callback',   []);
if nargin && ischar(varargin{1})
    gui_State.gui_Callback = str2func(varargin{1});
end

if nargout
    [varargout{1:nargout}] = gui_mainfcn(gui_State, varargin{:});
else
    gui_mainfcn(gui_State, varargin{:});
end
% End initialization code - DO NOT EDIT


% --- Executes just before PointingViewer is made visible.
function PointingViewer_OpeningFcn(hObject, eventdata, handles, varargin)
% This function has no output args, see OutputFcn.
% hObject    handle to figure
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    structure with handles and user data (see GUIDATA)
% varargin   command line arguments to PointingViewer (see VARARGIN)

% Choose default command line output for PointingViewer
handles.output = hObject;
handles.axes1.Color = [0 0 0];
handles.axes1.XTick = [];
handles.axes1.YTick = [];
handles.axes1.ZTick = [];
handles.moon_texture = dlmread('moon_texture.dat');

% Update handles structure
guidata(hObject, handles);

% UIWAIT makes PointingViewer wait for user response (see UIRESUME)
% uiwait(handles.figure1);


% --- Outputs from this function are returned to the command line.
function varargout = PointingViewer_OutputFcn(hObject, eventdata, handles)
% varargout  cell array for returning output args (see VARARGOUT);
% hObject    handle to figure
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    structure with handles and user data (see GUIDATA)

% Get default command line output from handles structure
varargout{1} = handles.output;


% --- Executes on button press in show_button.
function show_button_Callback(hObject, eventdata, handles)
% hObject    handle to show_button (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    structure with handles and user data (see GUIDATA)

try
    
    % break apart date string. Should have three parts
    date_pieces = strsplit(handles.date.String,'/');
    if length(date_pieces) ~= 3
        ME = MException('PointingViewer:PointingViewer:invalidDate',...
            'Bad date format: specify date using ''mm/dd/yyyy''');
        throw(ME);
    end
    
    % test all of them numbers
    for i = 1:3
        if ~all(isstrprop(date_pieces{i}, 'digit'))
            ME = MException('PointingViewer:PointingViewer:invalidDate',...
                'Bad date format: specify date using ''mm/dd/yyyy''');
            throw(ME);
        end
    end
    
    % get pieces
    yy = str2double(date_pieces{3});
    mm = str2double(date_pieces{1});
    dd = str2double(date_pieces{2});
    
    % month should be 1-12
    if mm > 12 || mm < 1
        ME = MException('PointingViewer:PointingViewer:invalidDate',...
            'Bad date: mm should be between 1 and 12 (inclusive)');
        throw(ME);
    end
    
    % days should be in month's range
    days_per_month = [31,28,31,30,31,30,31,31,30,31,30,31];
    if mod(yy,4) == 0
        days_per_month(2) = 29;
    end
    
    if dd > days_per_month(mm) || dd < 1
        ME = MException('PointingViewer:PointingViewer:invalidDate',...
            ['Bad date: for mm = ',num2str(mm),', dd should be between 1 and ',...
            num2str(days_per_month(mm))]);
        throw(ME);
    end
    
    % construct date integer
    date = yy*10^4 + mm*10^2 + dd;
    
    % break apart time string
    time_pieces = strsplit(handles.time.String,':');
    if length(time_pieces) ~= 2
        ME = MException('PointingViewer:PointingViewer:invalidTime',...
            'Bad time format: specify UT time using ''hh:mm''');
        throw(ME);
    end
    
    % test all of them numbers
    for i = 1:2
        if ~all(isstrprop(time_pieces{i}, 'digit'))
            ME = MException('PointingViewer:PointingViewer:invalidTime',...
                'Bad time format: specify UT time using ''hh:mm''');
            throw(ME);
        end
    end
    
    % get hour and minute
    hh = str2double(time_pieces{1});
    MM = str2double(time_pieces{2});
    
    % check for allowable values
    if hh > 23 || hh < 0
        ME = MException('PointingViewer:PointingViewer:invalidTime',...
            'Bad time: hh should be between 0 and 23');
        throw(ME);
    end
    
    if MM > 59 || MM < 0
        ME = MException('PointingViewer:PointingViewer:invalidTime',...
            'Bad time: mm should be between 0 and 59');
        throw(ME);
    end
    
    % construct time number
    time = hh*10^2 + MM;
    
    % get text box fields
    ew = str2double(handles.e_w.String);
    ns = str2double(handles.n_s.String);
    fov = str2double(handles.fov.String);
    
    % test east/west
    if isnan(ew)
        ME = MException('PointingViewer:PointingViewer:invalidEastWest',...
            'Bad east/west offset: value should be valid double');
        throw(ME);
    end
    
    % test north/south
    if isnan(ns)
        ME = MException('PointingViewer:PointingViewer:invalidNorthSouth',...
            'Bad north/south offset: value should be valid double');
        throw(ME);
    end
    
    % test FOV ang diam
    if isnan(fov)
        ME = MException('PointingViewer:PointingViewer:invalidFovDiam',...
            'Bad FOV diameter: value should be valid nonnegative double');
        throw(ME);
    elseif fov < 0
        ME = MException('PointingViewer:PointingViewer:invalidFovDiam',...
            'Bad FOV diameter: value should be nonnegative');
        throw(ME);
    end
    
    % get selected vals
    crater = handles.crater.String{handles.crater.Value};
    origin = handles.origin.String{handles.origin.Value};
    
    % calculate fov
    [lon,lat,alt,ra_c,dec_c,ra_fov,dec_fov] = see_pointing(crater,date,time,...
        ew,ns,origin,fov,handles.axes1,handles.moon_texture);
    
    if isnan(lon)
        handles.lon.String = '';
    else
        handles.lon.String = num2str(lon,'%7.1f');
    end
    
    if isnan(lat)
        handles.lat.String = '';
    else
        handles.lat.String = num2str(lat,'%7.1f');
    end
    
    if isnan(alt)
        handles.alt.String = '';
    else
        handles.alt.String = num2str(alt,'%8.1f');
    end
    
    doDiff = true;
    if isnan(dec_c)
        handles.crater_dec.String = '';
        doDiff = false;
    else
        handles.crater_dec.String = num2str(dec_c,'%8.3f');
    end
    
    if isnan(ra_c)
        handles.crater_ra.String = '';
        doDiff = false;
    else
        handles.crater_ra.String = num2str(ra_c,'%8.3f');
    end
    
    if isnan(dec_fov)
        handles.fov_dec.String = '';
        doDiff = false;
    else
        handles.fov_dec.String = num2str(dec_fov,'%8.3f');
    end
    
    if isnan(ra_fov)
        handles.fov_ra.String = '';
        doDiff = false;
    else
        handles.fov_ra.String = num2str(ra_fov,'%8.3f');
    end
    
    if doDiff
        diff_ra = ra_fov - ra_c;
        diff_dec = dec_fov - dec_c;
        
        handles.diff_ra.String = num2str(diff_ra,'%8.3f');
        handles.diff_dec.String = num2str(diff_dec,'%8.3f');
    end

    
catch ME
    cla
    me_ids = strsplit(ME.identifier,':');
    text(0.05,0.7,ME.message,'Color','white','Units','normalized','FontSize',20)
    if strcmp(me_ids{end},'webException')
        text(0.05,0.6,'Check connection to http://ssd.jpl.nasa.gov/horizons.cgi',...
            'Color','white','Units','normalized','FontSize',20)
    end
end

% --- Executes on selection change in crater.
function crater_Callback(hObject, eventdata, handles)
% hObject    handle to crater (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    structure with handles and user data (see GUIDATA)

% Hints: contents = cellstr(get(hObject,'String')) returns crater contents as cell array
%        contents{get(hObject,'Value')} returns selected item from crater


% --- Executes during object creation, after setting all properties.
function crater_CreateFcn(hObject, eventdata, handles)
% hObject    handle to crater (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    empty - handles not created until after all CreateFcns called

% Hint: listbox controls usually have a white background on Windows.
%       See ISPC and COMPUTER.
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end



function edit1_Callback(hObject, eventdata, handles)
% hObject    handle to edit1 (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    structure with handles and user data (see GUIDATA)

% Hints: get(hObject,'String') returns contents of edit1 as text
%        str2double(get(hObject,'String')) returns contents of edit1 as a double


% --- Executes during object creation, after setting all properties.
function edit1_CreateFcn(hObject, eventdata, handles)
% hObject    handle to edit1 (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    empty - handles not created until after all CreateFcns called

% Hint: edit controls usually have a white background on Windows.
%       See ISPC and COMPUTER.
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end



function date_Callback(hObject, eventdata, handles)
% hObject    handle to date (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    structure with handles and user data (see GUIDATA)

% Hints: get(hObject,'String') returns contents of date as text
%        str2double(get(hObject,'String')) returns contents of date as a double


% --- Executes during object creation, after setting all properties.
function date_CreateFcn(hObject, eventdata, handles)
% hObject    handle to date (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    empty - handles not created until after all CreateFcns called

% Hint: edit controls usually have a white background on Windows.
%       See ISPC and COMPUTER.
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end


function time_Callback(hObject, eventdata, handles)
% hObject    handle to time (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    structure with handles and user data (see GUIDATA)

% Hints: get(hObject,'String') returns contents of time as text
%        str2double(get(hObject,'String')) returns contents of time as a double


% --- Executes during object creation, after setting all properties.
function time_CreateFcn(hObject, eventdata, handles)
% hObject    handle to time (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    empty - handles not created until after all CreateFcns called

% Hint: edit controls usually have a white background on Windows.
%       See ISPC and COMPUTER.
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end


% --- Executes on selection change in origin.
function origin_Callback(hObject, eventdata, handles)
% hObject    handle to origin (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    structure with handles and user data (see GUIDATA)

% Hints: contents = cellstr(get(hObject,'String')) returns origin contents as cell array
%        contents{get(hObject,'Value')} returns selected item from origin


% --- Executes during object creation, after setting all properties.
function origin_CreateFcn(hObject, eventdata, handles)
% hObject    handle to origin (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    empty - handles not created until after all CreateFcns called

% Hint: listbox controls usually have a white background on Windows.
%       See ISPC and COMPUTER.
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end



function e_w_Callback(hObject, eventdata, handles)
% hObject    handle to e_w (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    structure with handles and user data (see GUIDATA)

% Hints: get(hObject,'String') returns contents of e_w as text
%        str2double(get(hObject,'String')) returns contents of e_w as a double


% --- Executes during object creation, after setting all properties.
function e_w_CreateFcn(hObject, eventdata, handles)
% hObject    handle to e_w (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    empty - handles not created until after all CreateFcns called

% Hint: edit controls usually have a white background on Windows.
%       See ISPC and COMPUTER.
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end



function n_s_Callback(hObject, eventdata, handles)
% hObject    handle to n_s (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    structure with handles and user data (see GUIDATA)

% Hints: get(hObject,'String') returns contents of n_s as text
%        str2double(get(hObject,'String')) returns contents of n_s as a double


% --- Executes during object creation, after setting all properties.
function n_s_CreateFcn(hObject, eventdata, handles)
% hObject    handle to n_s (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    empty - handles not created until after all CreateFcns called

% Hint: edit controls usually have a white background on Windows.
%       See ISPC and COMPUTER.
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end



function fov_Callback(hObject, eventdata, handles)
% hObject    handle to fov (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    structure with handles and user data (see GUIDATA)

% Hints: get(hObject,'String') returns contents of fov as text
%        str2double(get(hObject,'String')) returns contents of fov as a double


% --- Executes during object creation, after setting all properties.
function fov_CreateFcn(hObject, eventdata, handles)
% hObject    handle to fov (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    empty - handles not created until after all CreateFcns called

% Hint: edit controls usually have a white background on Windows.
%       See ISPC and COMPUTER.
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end



function alt_Callback(hObject, eventdata, handles)
% hObject    handle to alt (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    structure with handles and user data (see GUIDATA)

% Hints: get(hObject,'String') returns contents of alt as text
%        str2double(get(hObject,'String')) returns contents of alt as a double


% --- Executes during object creation, after setting all properties.
function alt_CreateFcn(hObject, eventdata, handles)
% hObject    handle to alt (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    empty - handles not created until after all CreateFcns called

% Hint: edit controls usually have a white background on Windows.
%       See ISPC and COMPUTER.
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end



function lat_Callback(hObject, eventdata, handles)
% hObject    handle to lat (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    structure with handles and user data (see GUIDATA)

% Hints: get(hObject,'String') returns contents of lat as text
%        str2double(get(hObject,'String')) returns contents of lat as a double


% --- Executes during object creation, after setting all properties.
function lat_CreateFcn(hObject, eventdata, handles)
% hObject    handle to lat (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    empty - handles not created until after all CreateFcns called

% Hint: edit controls usually have a white background on Windows.
%       See ISPC and COMPUTER.
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end



function lon_Callback(hObject, eventdata, handles)
% hObject    handle to lon (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    structure with handles and user data (see GUIDATA)

% Hints: get(hObject,'String') returns contents of lon as text
%        str2double(get(hObject,'String')) returns contents of lon as a double


% --- Executes during object creation, after setting all properties.
function lon_CreateFcn(hObject, eventdata, handles)
% hObject    handle to lon (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    empty - handles not created until after all CreateFcns called

% Hint: edit controls usually have a white background on Windows.
%       See ISPC and COMPUTER.
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end



function crater_ra_Callback(hObject, eventdata, handles)
% hObject    handle to crater_ra (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    structure with handles and user data (see GUIDATA)

% Hints: get(hObject,'String') returns contents of crater_ra as text
%        str2double(get(hObject,'String')) returns contents of crater_ra as a double


% --- Executes during object creation, after setting all properties.
function crater_ra_CreateFcn(hObject, eventdata, handles)
% hObject    handle to crater_ra (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    empty - handles not created until after all CreateFcns called

% Hint: edit controls usually have a white background on Windows.
%       See ISPC and COMPUTER.
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end



function fov_ra_Callback(hObject, eventdata, handles)
% hObject    handle to fov_ra (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    structure with handles and user data (see GUIDATA)

% Hints: get(hObject,'String') returns contents of fov_ra as text
%        str2double(get(hObject,'String')) returns contents of fov_ra as a double


% --- Executes during object creation, after setting all properties.
function fov_ra_CreateFcn(hObject, eventdata, handles)
% hObject    handle to fov_ra (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    empty - handles not created until after all CreateFcns called

% Hint: edit controls usually have a white background on Windows.
%       See ISPC and COMPUTER.
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end



function diff_ra_Callback(hObject, eventdata, handles)
% hObject    handle to diff_ra (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    structure with handles and user data (see GUIDATA)

% Hints: get(hObject,'String') returns contents of diff_ra as text
%        str2double(get(hObject,'String')) returns contents of diff_ra as a double


% --- Executes during object creation, after setting all properties.
function diff_ra_CreateFcn(hObject, eventdata, handles)
% hObject    handle to diff_ra (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    empty - handles not created until after all CreateFcns called

% Hint: edit controls usually have a white background on Windows.
%       See ISPC and COMPUTER.
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end



function crater_dec_Callback(hObject, eventdata, handles)
% hObject    handle to crater_dec (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    structure with handles and user data (see GUIDATA)

% Hints: get(hObject,'String') returns contents of crater_dec as text
%        str2double(get(hObject,'String')) returns contents of crater_dec as a double


% --- Executes during object creation, after setting all properties.
function crater_dec_CreateFcn(hObject, eventdata, handles)
% hObject    handle to crater_dec (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    empty - handles not created until after all CreateFcns called

% Hint: edit controls usually have a white background on Windows.
%       See ISPC and COMPUTER.
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end



function fov_dec_Callback(hObject, eventdata, handles)
% hObject    handle to fov_dec (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    structure with handles and user data (see GUIDATA)

% Hints: get(hObject,'String') returns contents of fov_dec as text
%        str2double(get(hObject,'String')) returns contents of fov_dec as a double


% --- Executes during object creation, after setting all properties.
function fov_dec_CreateFcn(hObject, eventdata, handles)
% hObject    handle to fov_dec (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    empty - handles not created until after all CreateFcns called

% Hint: edit controls usually have a white background on Windows.
%       See ISPC and COMPUTER.
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end



function diff_dec_Callback(hObject, eventdata, handles)
% hObject    handle to diff_dec (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    structure with handles and user data (see GUIDATA)

% Hints: get(hObject,'String') returns contents of diff_dec as text
%        str2double(get(hObject,'String')) returns contents of diff_dec as a double


% --- Executes during object creation, after setting all properties.
function diff_dec_CreateFcn(hObject, eventdata, handles)
% hObject    handle to diff_dec (see GCBO)
% eventdata  reserved - to be defined in a future version of MATLAB
% handles    empty - handles not created until after all CreateFcns called

% Hint: edit controls usually have a white background on Windows.
%       See ISPC and COMPUTER.
if ispc && isequal(get(hObject,'BackgroundColor'), get(0,'defaultUicontrolBackgroundColor'))
    set(hObject,'BackgroundColor','white');
end
