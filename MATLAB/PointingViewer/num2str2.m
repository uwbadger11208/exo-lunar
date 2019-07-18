function str = num2str2(num)
%UNTITLED5 Summary of this function goes here
%   Detailed explanation goes here
str = cell(size(num,1),size(num,2));
for i = 1:length(num)
if num(i) >= 10
    str{i} = num2str(num(i));
else
    str{i} = ['0',num2str(num(i))];
end
end
if numel(str) == 1
    str = str{1};
end
