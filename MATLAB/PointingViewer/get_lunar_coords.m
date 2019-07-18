function [xhat,yhat,zhat,origin,C,lunar_coords,ang_diam,sol_coords] = get_lunar_coords(date,time)

dn = dint2dn(date);
date_str1 = datestr(dn,'yyyy-mmm-dd');
hour1 = round(time - 50,-2)/100;
if hour1 < 0
    hour1 = 0;
end
min1 = mod(time,100);
time_str1 = [num2str2(hour1),':',num2str(min1)];

min2 = min1 + 1;
if min2 == 60
    hour2 = hour1 + 1;
    if hour2 == 24
        hour2 = 0;
        date_str2 = datestr(dn+1,'yyyy-mmm-dd');
    else
        date_str2 = date_str1;
    end
    min2 = 0;
else
    date_str2 = date_str1;
    hour2 = hour1;
end
time_str2 = [num2str2(hour2),':',num2str(min2)];
    
opt = weboptions;
opt.Timeout = 10;
try
z_axis_str = webread(['http://ssd.jpl.nasa.gov/horizons_batch.cgi?batch=1&',...
	'COMMAND=%27g:0,90,0@301%27&CENTER=%27695@399%27&OBJ_DATA=%27NO%27&',...
	'MAKE_EPHEM=%27YES%27&TABLE_TYPE=%27OBS%27&RANGE_UNITS=%27KM%27&',...
	'ANG_FORMAT=%27DEG%27&QUANTITIES=%271,20%27&',...
	'CSV_FORMAT=%27NO%27&STEP_SIZE=%272%20m%27&START_TIME=%27',...
    date_str1,'-',time_str1,'%27&STOP_TIME=%27',date_str2,'-',time_str2,'%27'],opt);
catch
    ME = MException('PointingViewer:get_lunar_coords:webException',...
        'Unable to retrieve lunar ephemeris data');
    throw(ME);
end

z_info = strsplit(get_ephem_sub(z_axis_str));
if (length(z_info{3}) == 1 || length(z_info{3}) == 2) && length(z_info{4}) == 2
    z_info(3) = [];
end
z_ra = str2double(z_info{4});
z_dec = str2double(z_info{5});
z_r = str2double(z_info{6});

z_pole = z_r*[cosd(z_dec)*cosd(z_ra),cosd(z_dec)*sind(z_ra),sind(z_dec)]';

try
x_axis_str = webread(['http://ssd.jpl.nasa.gov/horizons_batch.cgi?batch=1&',...
	'COMMAND=%27g:0,0,0@301%27&CENTER=%27695@399%27&OBJ_DATA=%27NO%27&',...
	'MAKE_EPHEM=%27YES%27&TABLE_TYPE=%27OBS%27&RANGE_UNITS=%27KM%27&',...
	'ANG_FORMAT=%27DEG%27&QUANTITIES=%271,20%27&',...
	'CSV_FORMAT=%27NO%27&STEP_SIZE=%272%20m%27&START_TIME=%27',...
    date_str1,'-',time_str1,'%27&STOP_TIME=%27',date_str2,'-',time_str2,'%27'],opt);
catch
    ME = MException('PointingViewer:get_lunar_coords:webException',...
        'Unable to retrieve lunar ephemeris data');
    throw(ME);
end

x_info = strsplit(get_ephem_sub(x_axis_str));
if (length(x_info{3}) == 1 || length(x_info{3}) == 2) && length(x_info{4}) == 2
    x_info(3) = [];
end
x_ra = str2double(x_info{4});
x_dec = str2double(x_info{5});
x_r = str2double(x_info{6});

x_pole = x_r*[cosd(x_dec)*cosd(x_ra),cosd(x_dec)*sind(x_ra),sind(x_dec)]';

try
origin_str = webread(['http://ssd.jpl.nasa.gov/horizons_batch.cgi?batch=1&',...
	'COMMAND=%27301%27&CENTER=%27695@399%27&OBJ_DATA=%27NO%27&',...
	'MAKE_EPHEM=%27YES%27&TABLE_TYPE=%27OBS%27&RANGE_UNITS=%27KM%27&',...
	'ANG_FORMAT=%27DEG%27&QUANTITIES=%271,13,15,20%27&',...
	'CSV_FORMAT=%27NO%27&STEP_SIZE=%272%20m%27&START_TIME=%27',...
    date_str1,'-',time_str1,'%27&STOP_TIME=%27',date_str2,'-',time_str2,'%27'],opt);
catch
    ME = MException('PointingViewer:get_lunar_coords:webException',...
        'Unable to retrieve lunar ephemeris data');
    throw(ME);
end

o_info = strsplit(get_ephem_sub(origin_str));
if length(o_info{3}) == 1 || length(o_info{3}) == 2
    o_info(3) = [];
end
o_ra = str2double(o_info{3});
o_dec = str2double(o_info{4});
ang_diam = str2double(o_info{5});
sol_lon = str2double(o_info{6});
sol_lat = str2double(o_info{7});
sol_coords = [sol_lon,sol_lat];
o_r = str2double(o_info{8});

lunar_coords = [o_ra,o_dec];

origin = o_r*[cosd(o_dec)*cosd(o_ra),cosd(o_dec)*sind(o_ra),sind(o_dec)]';

z_axis = z_pole - origin;
x_axis = x_pole - origin;

zhat = z_axis / norm(z_axis);
xhat = x_axis / norm(x_axis);
yhat = cross(zhat,xhat);

C = [xhat,yhat,zhat];

end