function dn = dint2dn(date_integer)

dn = zeros(size(date_integer));

for i = 1:size(date_integer,1)
    for j = 1:size(date_integer,2)
        date_str = num2str(date_integer(i,j));
        if length(date_str) ~= 8
           
            for k = (length(date_str)+1):6
                date_str = ['0',date_str];
            end
            yy = str2double(date_str(1:2));
            mm = str2double(date_str(3:4));
            dd = str2double(date_str(5:6));
            dn(i,j) = datenum(2000 + yy,mm,dd);
        else
           
            yy = str2double(date_str(1:4));
            mm = str2double(date_str(5:6));
            dd = str2double(date_str(7:8));
            dn(i,j) = datenum(yy,mm,dd);
        end
    end
end