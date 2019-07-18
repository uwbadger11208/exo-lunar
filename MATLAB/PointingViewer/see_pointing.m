function [lon,lat,alt,ra_c,dec_c,ra_fov,dec_fov] = see_pointing(crater,date,time,ew,ns,origin,fov,ax,moon_texture)

if nargin < 8
    ax = gca;
end

set(gcf,'CurrentAxes',ax)

cla

LUNAR_RAD = 1737.53;
EDGE_DIAM = 412.5;
mesh = 2^8;

text(0.05,0.5,'retrieving lunar ephemeris data','Units','normalized','Color',...
    'white','FontSize',20);
drawnow

[xhat,yhat,zhat,ovec,Cm2i,lun_coords,ang_diam,sol_coords] = get_lunar_coords(date,time);

cla
text(0.05,0.5,'lunar ephemeris data retrieved','Units','normalized','Color',...
    'white','FontSize',20);
drawnow

selen_crater_vec = selenocentric_crater_coords(crater,LUNAR_RAD);
if any(isnan(Cm2i))
    disp('Bad lunar values')
    return
end

inert_crater_vec = ovec + Cm2i*selen_crater_vec;
icv_hat = inert_crater_vec / norm(inert_crater_vec);
inert_crater_coords(2) = asind(icv_hat(3));
inert_crater_coords(1) = atan2d(icv_hat(2),icv_hat(1));

% pull out ra,dec, explicitly
dec_c = inert_crater_coords(2);
ra_c = inert_crater_coords(1);

ori = strsplit(origin);
point_origin = ori{1};
if length(ori) > 1
direction = ori{2};
end
switch point_origin
    case 'Crater'
        start_coords = inert_crater_coords;
    case 'Limb'
        start_coords = limb_coords(inert_crater_coords,direction,...
            fov,ang_diam,ovec);
    case 'Edge'
        start_coords = limb_coords(inert_crater_coords,direction,...
            EDGE_DIAM,ang_diam,ovec);
    otherwise
        disp('bad origin value')
        return
end

fov_coords(1) = start_coords(1) + (ew / 240);
fov_coords(2) = start_coords(2) + (ns / 60);

% pull out FOV ra, dec
ra_fov = fov_coords(1);
dec_fov = fov_coords(2);

%%%%%%%%%%%%%%%%%%%

[LON,LAT] = meshgrid(linspace(-180,180,2*mesh + 1),linspace(-90,90,2*mesh + 1));
LON = LON(:,:,[1,1,1]);
LAT = LAT(:,:,[1,1,1]);

xp_hat = -ovec / norm(ovec);
yp_dir = cross([0,0,1]',xp_hat);
yp_hat = yp_dir / norm(yp_dir);
zp_hat = cross(xp_hat,yp_hat);
Cp2i = [xp_hat,yp_hat,zp_hat]; % plot 2 inertial

m_hats = {ones(size(LON,1),size(LON,2),3)};
m_hats{2} = m_hats{1};
m_hats{3} = m_hats{1};

m_hat{1} = Cp2i'*xhat; %X
m_hat{2} = Cp2i'*yhat; %Y
m_hat{3} = Cp2i'*zhat; %Z

for i = 1:3
    for j = 1:3
        m_hats{i}(:,:,j) = m_hat{i}(j);
    end
end


moon_vec = LUNAR_RAD*(cosd(LAT).*cosd(LON).*m_hats{1} + ...
    cosd(LAT).*sind(LON).*m_hats{2} + sind(LAT).*m_hats{3});

cla
h = surf(ax,moon_vec(:,:,1),moon_vec(:,:,2),moon_vec(:,:,3));
set(h,'FaceColor','texturemap','EdgeColor','none');
%set(gcf,'Color',[0 0 0])

if nargin < 9
    moon_texture = dlmread('moon_texture.dat');
end

set(h,'CData',moon_texture)

sol_vec = Cm2i*unit_vec(sol_coords(1),sol_coords(2));
ys_dir = cross([0,0,1]',sol_vec);
ys_hat = ys_dir / norm(ys_dir);
xs_hat = cross(ys_hat,sol_vec);

LAT_dark = LAT(1:(mesh+1),:,:);
LON_dark = LON(1:(mesh+1),:,:);
%{
[LON_dark,LAT_dark] = meshgrid(linspace(-180,180,mesh),linspace(-90,0,mesh));
LON_dark = LON_dark(:,:,[1,1,1]);
LAT_dark = LAT_dark(:,:,[1,1,1]);
%}

s_hats = {ones(size(LON_dark,1),size(LON_dark,2),3)};
s_hats{2} = s_hats{1};
s_hats{3} = s_hats{1};

s_hat{1} = Cp2i'*xs_hat; %X
s_hat{2} = Cp2i'*ys_hat; %Y
s_hat{3} = Cp2i'*sol_vec; %Z

for i = 1:3
    for j = 1:3
        s_hats{i}(:,:,j) = s_hat{i}(j);
    end
end

dark_vec = (LUNAR_RAD+2)*(cosd(LAT_dark).*cosd(LON_dark).*s_hats{1} + ...
    cosd(LAT_dark).*sind(LON_dark).*s_hats{2} + sind(LAT_dark).*s_hats{3});

hold on
h_d = surf(ax,dark_vec(:,:,1),dark_vec(:,:,2),dark_vec(:,:,3));
set(h_d,'EdgeColor','none','CData',ones(size(dark_vec(:,:,1)))*double(min(moon_texture(:))));

if ~strcmp(crater,'Moon View')
    c = get(gca,'ColorOrder');
    
    theta = acosd(dot(unit_vec(fov_coords(1),fov_coords(2)),unit_vec(lun_coords(1),lun_coords(2))));
    
    fov_inertial_vec = norm(ovec)*cosd(theta)*unit_vec(fov_coords(1),fov_coords(2));
    
    fov_inertial_zhat = unit_vec(fov_coords(1),fov_coords(2));
    fov_inertial_ydir = cross([0,0,1]',fov_inertial_zhat);
    fov_inertial_yhat = fov_inertial_ydir ./ norm(fov_inertial_ydir);
    fov_inertial_xhat = cross(fov_inertial_zhat,fov_inertial_yhat);
    
    fov_plot_vec = Cp2i'*(fov_inertial_vec - ovec);
    %fov_plot_zhat = Cp2i'*fov_inertial_zhat;
    fov_plot_yhat = Cp2i'*fov_inertial_yhat;
    fov_plot_xhat = Cp2i'*fov_inertial_xhat;
    fov_norm = LUNAR_RAD*(fov / ang_diam);
    
    th = linspace(0,2*pi,100);
    fov_circle_x = fov_plot_vec(1) + fov_norm*(fov_plot_xhat(1)*cos(th) + fov_plot_yhat(1)*sin(th));
    fov_circle_y = fov_plot_vec(2) + fov_norm*(fov_plot_xhat(2)*cos(th) + fov_plot_yhat(2)*sin(th));
    fov_circle_z = fov_plot_vec(3) + fov_norm*(fov_plot_xhat(3)*cos(th) + fov_plot_yhat(3)*sin(th));
    
    fov_circle_x_2 = 2*LUNAR_RAD + fov_norm*(fov_plot_xhat(1)*cos(th) + fov_plot_yhat(1)*sin(th));
    
    plot_crater = num2cell(Cp2i'*Cm2i*selen_crater_vec);
    plot_crater{1} = linspace(plot_crater{1},LUNAR_RAD*2,2);
    plot_crater{2} = plot_crater{2}([1 1]);
    plot_crater{3} = plot_crater{3}([1 1]);
    plot3(ax,plot_crater{:},'.','MarkerSize',20,'Color',c(2,:))
    plot3(ax,fov_circle_x,fov_circle_y,fov_circle_z,'LineWidth',3,'Color',c(1,:))
    plot3(ax,fov_circle_x_2,fov_circle_y,fov_circle_z,'LineWidth',3,'Color',c(1,:))

    fov_moon_vec = Cm2i'*(fov_inertial_vec - ovec);
    lat = asind(fov_moon_vec(3) / norm(fov_moon_vec));
    lon = atan2d(fov_moon_vec(2),fov_moon_vec(1));
    alt = norm(fov_moon_vec) - LUNAR_RAD;
else
    lon = NaN;
    lat = NaN;
    alt = NaN;
end

axis equal
view([1,0,0])
colormap gray
ax.Color = [0 0 0];
ax.XTick = [];
ax.YTick = [];
ax.ZTick = [];
end

function vec = unit_vec(ra,dec)
vec = [cosd(dec)*cosd(ra),cosd(dec)*sind(ra),sind(dec)]';
end
