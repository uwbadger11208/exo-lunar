function [vec,coords] = selenocentric_crater_coords(crater,lunar_rad)

switch crater
		case 'Langrenus'   
            coords = [ 60.9, -8.9];
		case 'Cleomedes'   
            coords = [ 55.5, 27.7];
		case 'Petavius'    
            coords = [ 60.4,-25.3];
		case 'Grimaldi'    
            coords = [-68.8, -5.2]; % 291.2
		case 'Aristarchus' 
            coords = [-47.4, 23.7]; % 312.6
		case 'Tycho'       
            coords = [-11.4,-43.3]; % 348.6
		case 'Plato'       
            coords = [ -9.3, 51.6]; % 350.7
		case 'Apollonius'  
            coords = [ 61.1,  4.5];
		case 'Endymion'    
            coords = [ 56.5, 53.6];
		case 'Messala'     
            coords = [ 59.9, 39.2];
		case 'Atlas'       
            coords = [ 44.4, 46.7];
		case 'Janssen'     
            coords = [ 40.8,-45.0];
		case 'Ptolemaeus'  
            coords = [ -1.8, -9.2]; % 358.2
		case 'Kepler'      
            coords = [-38.0,  8.1]; % 322.0
		case 'Copernicus'  
            coords = [-20.1,  9.6]; % 339.9
		case 'Gassendi'    
            coords = [-40.0,-17.6]; % 320.0
		case 'Mare Iridum' 
            coords = [-31.5, 44.1]; % 328.5
		case 'Theophilus'  
            coords = [ 26.4,-11.4];
		case 'Godin'       
            coords = [ 10.2,  1.8];
		case 'Vieta'       
            coords = [-56.3,-29.2]; % 303.7
		case 'Furnerius'   
            coords = [ 60.4,-36.3];
		case 'Schickard'   
            coords = [-54.6,-44.4];
		case 'Proclus'     
            coords = [ 46.8, 16.1];
        case 'Moon View'
            coords = [  0.0,  0.0];
		case 'Moon Center' 
            coords = [  0.0,  0.0];
end

vec = lunar_rad*[cosd(coords(2))*cosd(coords(1)),cosd(coords(2))*sind(coords(1)),sind(coords(2))]';