function sub = get_ephem_sub(ephem_str)

start_ind = strfind(ephem_str,'$$SOE') + 7;
end_ind = strfind(ephem_str,'$$EOE') - 2;

sub = ephem_str(start_ind:end_ind);

end