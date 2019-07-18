function coords = limb_coords(start_coords,direction,fov,ang_diam,ovec)

ohat = ovec / norm(ovec);

ra = start_coords(1);
dec = start_coords(2);

dot_prod = cosd((fov+ang_diam)/(2*3600));

minTol = 10^-10;

switch direction
    
    case 'N'
        d1 = dec;
        d2 = dec;
        while dot(ohat,unit_vec(ra,d2)) > dot_prod
            d2 = d2 + 1;
        end
        
        while abs(dot(ohat,unit_vec(ra,(d1+d2)/2)) - dot_prod) > minTol
            if dot(ohat,unit_vec(ra,(d1+d2)/2)) > dot_prod
                d1 = (d1 + d2) / 2;
            else
                d2 = (d1 + d2) / 2;
            end
        end
        
        dec = (d1 + d2) / 2;
        
    case 'S'
        
        d1 = dec;
        d2 = dec;
        while dot(ohat,unit_vec(ra,d2)) > dot_prod
            d2 = d2 - 1;
        end
        
        while abs(dot(ohat,unit_vec(ra,(d1+d2)/2)) - dot_prod) > minTol
            if dot(ohat,unit_vec(ra,(d1+d2)/2)) > dot_prod
                d1 = (d1 + d2) / 2;
            else
                d2 = (d1 + d2) / 2;
            end
        end
        
        dec = (d1 + d2) / 2;
        
    case 'E'
        
        r1 = ra;
        r2 = ra;
        while dot(ohat,unit_vec(r2,dec)) > dot_prod
            r2 = r2 + 1;
        end
        
        while abs(dot(ohat,unit_vec((r1 + r2) / 2,dec)) - dot_prod) > minTol
            if dot(ohat,unit_vec((r1 + r2) / 2,dec)) > dot_prod
                r1 = (r1 + r2) / 2;
            else
                r2 = (r1 + r2) / 2;
            end
        end
        
        ra = (r1 + r2) / 2;
        
    case 'W'
        
        r1 = ra;
        r2 = ra;
        %disp(['r2: ',num2str(r2)])
        while dot(ohat,unit_vec(r2,dec)) > dot_prod
            r2 = r2 - 1;
            %disp(['r2: ',num2str(r2)])
        end
        
        while abs(dot(ohat,unit_vec((r1 + r2) / 2,dec)) - dot_prod) > minTol
            if dot(ohat,unit_vec((r1 + r2) / 2,dec)) > dot_prod
                r1 = (r1 + r2) / 2;
                %disp(['r1: ',num2str(r1)])
            else
                r2 = (r1 + r2) / 2;
                %disp(['r2: ',num2str(r2)])
            end
        end
        
        ra = (r1 + r2) / 2;      
end

coords = [ra,dec];

end

function vec = unit_vec(ra,dec)
vec = [cosd(dec)*cosd(ra),cosd(dec)*sind(ra),sind(dec)]';
end