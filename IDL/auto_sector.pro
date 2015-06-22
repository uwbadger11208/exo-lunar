 ;----------------------------------------------------------------------------
 ; AUTO_SECTOR.PRO
 ;
 ; USAGE:
 ;   auto_sector, image [, nsectors, path=, bias=, flat=, x0=, y0=, xr=, yr=,$
 ;     maxdp=, rdp1=, spc= OR /spc, /showim, /noplot, /ft, /ps, /vft, /all,
 ;     /center, /rank, /coarse, /medium, /fine, /noauto, mindp=
 ;
 ; PARAMETERS:
 ;   image: the name of the FITS file to be examined (e.g. 'moon_02.fits')
 ;   nsectors: the number of sectors with which to examine "image." If
 ;     omitted, nsectors is set to 1 and a normal ring sum is performed
 ;
 ; OPTIONAL PARAMETERS:
 ;   path: the path from the current directory to the directory
 ;     containing "image"
 ;   bias: the FITS file representing a bias to be subtracted from "image"
 ;   flat: the FITS file representing a flat field with which to correct
 ;     "image"
 ;   x0: the x-coordinate of the ring center
 ;   y0: the y-coordinate of the ring center
 ;     NOTE: this submitted center overrides the default center hard-coded
 ;       into the first lines of AUTO_SECTOR
 ;   xr: a 2-element vector representing the desired x-range of any
 ;     produced spectrum plots. Also defines the range over which the
 ;     '/center' option fits the data to a Gaussian
 ;   yr: a 2-element vector representing the desired y-range of any
 ;     produced spectrum plots
 ;   spc: produces a .spc file for each sector with the naming convention
 ;     <spc>_<sector number>.spc. (If nsectors is 1, '_<sector number>' is
 ;     omitted.)
 ;
 ; KEYWORDS:
 ;   /spc: has the same effect as setting spc equal to the portion of "image"
 ;     before '.fits'
 ;   /showim: displays the bias-subtracted, flatfield-corrected image
 ;   /noplot: suspends the creation of the color-coded spectra plot
 ;   /ps: writes a PS image of the color-coded spectra plot and key, named
 ;     '<name>_key.ps' and '<name>_spc.ps'. If nsectors is 1, only one
 ;     spectrum is produced, called '<name>.ps'
 ;   /ft: produces a .ft file to be loaded into VoigtFit. If run in
 ;     a directory containing each sector's .spc file and a start.par file,
 ;     VoigtFit will produce a .vft file representing the continuum of each
 ;     sector's spectrum
 ;   /vft: reads in the .vft file for each spectrum, subtracts each
 ;     sector's background continuum, averages the results, and writes a
 ;     final .spc file for the image
 ;   /center: the program will find the best center in a 3x3 1-pixel grid,
 ;     then a 5x5 .5-pixel grid, then a 7x7 .1-pixel grid based on the
 ;     proximity of the centers of Gaussian fits of each setctors' spectrum.
 ;     If nsectors is equal to 1, smallest peak width will be used instead.
 ;     Parameters can be altered using the /coarse, /medium, /fine, and
 ;     /noauto keywords
 ;   /rank: produces a list of ever center considered in the final centering
 ;     grid, ranked from best to worst.
 ;   /coarse: causes the centering algorithm to stop after the 3x3 search
 ;   /medium: causes the centering algorithm to stop after the 5x5 search
 ;   /fine: causes the centering algorithm to continue after the 7x7 search
 ;     and perform a 9x9 .05-pixel search
 ;   /noauto: when called in conjuction with /coarse, /medium, or /fine,
 ;     causes the program to begin and end in the same grid. That is, while
 ;     a call to /medium with yield a 3x3 search and then a 5x5 search, a
 ;     call to /medium and /noauto will yield only a 5x5 search. A call only
 ;     to /noauto will yield only a 7x7 search.
 ;----------------------------------------------------------------------------

pro auto_sector,image,nsectors,path=path,bias=bias,flat=flat,x0=x0,y0=y0, $
    xr=xr,yr=yr,spc=spc,showim=showim,noplot=noplot,ps=ps,ft=ft,vft=vft, $
    center=center,rank=rank,coarse=coarse,medium=medium,fine=fine,$
    noauto=noauto, all=all, maxdp=maxdp, r1dp=r1dp, best_center=best_center,$
    plotring=plotring

; specify circle center
if n_elements(x0) eq 0 then x0=252.9
if n_elements(y0) eq 0 then y0=268.9

; make certain centers are not integers
x0 = x0*1.
y0 = y0*1.

; define radius of first data point and maximum number of data points
if n_elements(r1dp) eq 0 then r1dp = 12
if n_elements(maxdp) eq 0 then maxdp = 136

; number of sigma for checking pixel values
nsigma =  3.0

; handle nsectors
if n_elements(nsectors) eq 0 then nsectors = 1

; handle path
if n_elements(path) eq 0 then path=''

; import image
if n_elements(image) eq 0 then begin
  print, 'No image supplied'
  return
endif
im=readfits(path+image,/silent)
if im[0] eq -1 then begin
  print, 'Image ' + path + image + ' not found'
  return
endif
im = im*1.

; pull out image name
name = strmid(image,0,strlen(image)-5)

; pull out image size
imagesize = size(im)
arraydimx = imagesize[1]
arraydimy = imagesize[2]

; read in bias if provided
if n_elements(bias) eq 0 then begin
  biasUse = 'no'
endif else begin
  bi = readfits(path+bias,/silent)
  biasUse = 'yes'
  if bi[0] eq -1 then begin
    print, 'Bias ' + path + bias + ' not found'
    return
  endif
  im = im - bi
endelse

; read in flatfield if wanted
if n_elements(flat) eq 0 then begin
  flatUse = 'no'
endif else begin
  fl = readfits(path+flat,/silent)
  flatUse = 'yes'
  if fl[0] eq -1 then begin
    print, 'Flat ' + path + flat + ' not found'
    return
  endif
  im = im / fl
endelse

; if centering, eliminate any non-compatible options chosen
if n_elements(center) ne 0 then begin
  noplot = 1
  delvar, spc
  delvar, ps
  delvar, ft
  delvar, vft
endif

; check for all keyword
if n_elements(all) ne 0 then begin
  spc = 1
  vft = 1
  ft = 1
endif

; if color is needed, set corresponding variables to their correct values
if n_elements(noplot) eq 0 or n_elements(ps) ne 0 or $
    n_elements(center) ne 0 then begin
  usecolor = 1
  if n_elements(center) ne 0 or n_elements(noplot) eq 0 then begin
    makeIm = 1
  endif else begin
    makeIm = 0
  endelse
endif else begin
  usecolor = 0
  makeIm = 0
endelse


; get variables for the various spectra
bin = indgen(maxdp)
binvals = fltarr(maxdp,nsectors)

; show original image
if n_elements(showim) ne 0 then begin
  device, decomposed=0
  loadct, 0, /silent
  window, 0, xs=arraydimx, ys=arraydimy
  tvscl, im
endif

if n_elements(usecolor) ne 0 and nsectors gt 1 then begin

  ; get variables for color info
  colors = fltarr(nsectors)
  reds=fltarr(nsectors)
  blues=fltarr(nsectors)
  greens=fltarr(nsectors)

  ; load rainbow + white table, assign colors to sectors
  loadct, 39, /silent
  tvlct, r, g, b, /get
  csteps = 255 / nsectors
  for n = 0,nsectors -1 do begin
    colors[n] = 254 - n*csteps
    reds[n] = r[colors[n]]
    blues[n] = b[colors[n]]
    greens[n] = g[colors[n]]
  endfor

  ; create vectors to interpolate for custom table
  ; two colors example: black/white is scaled from 0-253,
  ; 254 is color one and 255 is color two
  redInterp = indgen(nsectors + 2)
  blueInterp = indgen(nsectors + 2)
  greenInterp = indgen(nsectors + 2)
  interpInd = indgen(nsectors + 2)
  for i = 0, nsectors - 1 do begin
    redInterp[i + 2] = reds[i]
    blueInterp[i + 2] = blues[i]
    greenInterp[i + 2] = greens[i]
    interpInd[i + 2] = 255 - (nsectors - 1) + i
  endfor
  for i = 0,1 do begin
    redInterp[i] = i*255
    blueInterp[i] = i*255
    greenInterp[i] = i*255
    interpInd[i] = i*(255 - nsectors)
  endfor

  ; create vectors representing custom color table
  rToDo = long(interpol(redInterp,interpInd,findgen(256)))
  bToDo = long(interpol(blueInterp,interpInd,findgen(256)))
  gToDo = long(interpol(greenInterp,interpInd,findgen(256)))
endif

; do the sector sum and return
goto, pbo
pboback:
; after the sum, the variables bins, binvals contain the x,y pairs
; representing the new spectrum, while newIm contains the color-coded key

; if required, create the color-coded key (normal image if nsectors = 1)
if makeIm eq 1 then begin

  ; display portion of interest of newIm
  window, 1, xs = arraydimx, ys = arraydimy
  device, decomposed=0

  ; only use color if more than one sector
  if nsectors gt 1 then begin
    tvlct,rToDo,gToDo,bToDo
  endif else begin
    loadct, 0, /silent
  endelse

  ; display the image, its name, and whether it was bias/flat fielded
  tvscl, newIm
  xyouts, .45, .9, /normal, font = 0, name, color = white
  xyouts, .1, .1, /normal, font = 0, 'Bias subtracted?   ' + biasUse, $
      color = white
  xyouts, .6, .1, /normal, font = 0, 'Flatfield reduced?    ' + flatUse, $
      color = white

  ; note that the image has been produced
  makeIm = 0
endif

; create color-coded spectrum plot if desired
if n_elements(noplot) eq 0 then begin

  ; display various spectra, color coded
  window, 2
  device, decomposed=0
  loadct, 0, /silent

  ; plot first sector's spectrum
  plot,bin,binvals[*,0],xrange=xr,yrange=yr,xstyle=1

  ; if nsectors > 1, oplot each sector's spectrum in its assigned color
  if nsectors gt 1 then begin
    loadct, 39, /silent
    for n = 0, nsectors - 1 do begin
      oplot,bin, binvals[*,n], color = colors[n]
    endfor
  endif
endif

; create PS plot of spectra and key if desired
if n_elements(ps) ne 0 then begin
  ; display portion of interest of newIm
  set_plot,'PS'

  ; only make key (containing same info as X windows version)
  ; if the number of sectors is greater than one
  if nsectors gt 1 then begin
    device, filename = name + '_key.ps', /color, bits=8, decomposed = 0

    ; load custom table, display image
    tvlct,rToDo,gToDo,bToDo
    tvscl, newIm
    ; display in image whether bias and flat were used
    xyouts, .32, .9, /normal, font = 0, name, color = white
    xyouts, .08, .1, /normal, font = 0, 'Bias subtracted?   ' + biasUse, $
        color = white
    xyouts, .42, .1, /normal, font = 0, 'Flatfield reduced?    ' + flatUse, $
        color = white
    device, /close
  endif

  ; display various spectra, color coded

  ; drop '_spc' suffix if nsectors is one
  if nsectors gt 1 then begin
    device, filename = name + '_spc.ps', /color, decomposed=0, /landscape
  endif else begin
    device, filename = name + '.ps', /color, decomposed=0, /landscape
  endelse
  loadct, 0, /silent

  ; use same plotting algorithm seen above
  plot,bin,binvals[*,0],xrange=xr,yrange=yr
  if nsectors gt 1 then begin
    loadct, 39, /silent
    for n = 0, nsectors - 1 do begin
      oplot,bin, binvals[*,n], color = colors[n]
    endfor
  endif
  device, /close, /portrait
  set_plot, 'X'
endif

; if required, derive names for .spc files
if n_elements(spc) ne 0 or n_elements(ft) ne 0 then begin
  ; create and fill array of the filenames for the spectra
  spcnames = strarr(nsectors)

  ; if spc is a string, use that as name of spectra
  if size(spc,/type) eq 7 then begin
    prefix = spc

  ; otherwise, use image name
  endif else begin
    prefix = name
  endelse

  ; if nsectors is one, use simple name for file. Otherwise, number by sector
  if nsectors eq 1 then begin
    spcnames[0] = prefix + '.spc'
  endif else begin
    for sector = 0, nsectors-1 do begin
      if sector lt 9 then begin
        spcnames[sector] = prefix + '_0' + strcompress(sector + 1,/remove_all) $
            + '.spc'
      endif else begin
        spcnames[sector] = prefix + '_' + strcompress(sector+1,/remove_all) $
            + '.spc'
      endelse
    endfor
  endelse
endif

; if required, create .spc files
if n_elements(spc) ne 0 then begin

  ; create each .spc file
  for n = 0, nsectors - 1 do begin
    spcname = spcnames[n]
    openw, lun, spcname, /get_lun
    printf, lun, '# HEADER GOES HERE'
    if n_elements(xr) eq 0 then begin
    	spc_low = 0
    	spc_high = maxdp - 1
    endif else begin
    	spc_low = xr[0]
    	spc_high = xr[1]
    	if spc_low lt 0 then spc_low = 0
    	if spc_high gt maxdp - 1 then spc_high = maxdp - 1
    	if spc_low ge spc_high then begin
    		spc_low = 0
    		spc_high = maxdp - 1
    	endif
    endelse
    for i = spc_low,spc_high do begin
      printf, lun, i - spc_low, binvals[i,n]
    endfor
    close, lun
    free_lun, lun
  endfor
endif

; if creating .ft file for rapid fitting
if n_elements(ft) ne 0 then begin

  ; determine filename
  runfile = name + '.ft'
  openw, lun, runfile, /get_lun

  ; print VoigtFit commands
  for n = 0, nsectors - 1 do begin

    ; get data to fit
    printf, lun, 'getd ' + spcnames[n]

    ; load parameters - first time start.par, then previous .par file
    if n eq 0 then begin
      printf, lun, 'getp start.par'
    endif else begin
      if n lt 10 then begin
        printf, lun, 'getp ' + name + '_0' + strcompress(n,/remove_all) $
            + '.par'
      endif else begin
        printf, lun, 'getp ' + name + '_' + strcompress(n,/remove_all) $
            + '.par'
      endelse
    endelse

    ; fit 20 times
    for i = 0, 20 do begin
      printf, lun, 'fitv'
    endfor

    ; save .par file to load for next data
    printf, lun, 'savep'

    ; save graph of fit to check
    printf, lun, 'plotf'
    printf, lun, 'saveg'

    ; remove all voigts but 'invisble' 1st voigt to left of origin
    printf, lun, 'setm 1 2'

    ; save graph of continuum to check
    printf, lun, 'plotf'
    if n lt 9 then begin
      printf, lun, 'fsaveg ' + name + '_0' + strcompress(n + 1,/remove_all) $
            + '_2.ps'
    endif else begin
      printf, lun, 'fsaveg ' + name + '_' + strcompress(n + 1,/remove_all) $
            + '_2.ps'
    endelse

    ; save continuum as .vft file
    printf, lun, 'savef'
  endfor
  close, lun
  free_lun, lun
endif

if n_elements(all) ne 0 then spawn,'vf ' + runfile

; if reading in vft's for final spc
if n_elements(vft) ne 0 then begin

  ; create arrays for continuums
  continuums = fltarr(maxdp,nsectors)
  fitnames = strarr(nsectors)

  ; produce names for .vft files to read in
  for sector = 0, nsectors-1 do begin
    if sector lt 9 then begin
      fitnames[sector] = name + '_0' + strcompress(sector + 1,/remove_all) $
          + '.vft'
    endif else begin
      fitnames[sector] = name + '_' + strcompress(sector+1,/remove_all) $
          + '.vft'
    endelse
  endfor

  ; read in continuums
  for n = 0, nsectors - 1 do begin
    fitname = fitnames[n]

    ; get rid of header
    trash = strarr(4)
    openr,lun,fitname,/get_lun
    readf, lun, trash

    ; read in continuum
    for d = 0,maxdp - 1 do begin
      dataIn = fltarr(4,1)
      readf,lun,dataIn
      continuums[d,n] = dataIn[1,0]
    endfor
    close, lun
    free_lun, lun
  endfor

  ; plot each spectra with its continuum subtracted
  window, 4
  device, decomposed=0
  loadct,39,/silent

  ; plot first spectrum in white
  plot,bin,binvals[*,0] - continuums[*,0]

  ; oplot with color
  for n = 0, nsectors - 1 do begin
    oplot,bin, binvals[*,n] - continuums[*,n], color = colors[n]
  endfor

  ; create variable for final spc data
  vals = fltarr(maxdp)
  for n = 0,maxdp-1 do begin
    vals[n] = mean(binvals[n,*] - continuums[n,*])
  endfor

  ; plot final spc data
  window, 5
  vals = vals
  plot,bin,vals+mean(binvals)

  ; clean up
  spawn, 'rm ' + name + '*.spc'
  spawn, 'rm ' + name + '.spc'
  spawn, 'rm ' + name + '*.vft'
  spawn, 'rm ' + name + '*.ft'
  spawn, 'rm ' + name + '*.par'
  spawn, 'rm ' + name + '*.ps'

  ; write final .spc file
  ofile = name + '.spc'
  openw, lun, ofile, /get_lun
  for i = 0,maxdp - 1 do begin
    printf, lun, bin[i], vals[i]
  endfor
  close, lun
  free_lun, lun

endif

; if /center is specified, program moves to centering algorithm here
if n_elements(center) ne 0 then goto, centering
centerback:

goto, over

;*****************************************************************************
;   RING SUMMING CODE
;*****************************************************************************

; initial ring sum
pbo:

; declare variables as needed for correct program flow
toCenter = 0

; declare color-coded key image - only produced first time through
newIm = fltarr(arraydimx,arraydimy)

; ring sum repeated in centering algorithm
pbocenter:

; declare variable as needed for correct program flow
final = 0

; ring sum for final width/height info in centering algorithm
pbocenterfinal:

; create arrays for pixel position
whichbin=lonarr(arraydimx,arraydimy)
theta=fltarr(arraydimx,arraydimy)

; calculate the area of the first data point (pi's drop out in area ratio)
a1dp=(r1dp)^2

; go through image and assign pixels to ring bins and theta map
for y=0L,arraydimy-1 do begin
  for x=0L,arraydimx-1 do begin

    ; pull radius of point (really area of circle with that radius
    ; and theta at (x,y) from complex number representation
    r = x*1. - x0
    i = y*1. - y0
    ap=(r^2+i^2)
    theta[x,y]=atan(complex(r,i),/PHASE)

    ; change theta->(-PI,PI) to theta->(0,2*PI)
    if theta[x,y] lt 0 then theta[x,y] = theta[x,y] + 2*!PI
    ; ring bin is ratio of areas
    b=long(ap/a1dp)
    whichbin(x,y)=b
  endfor
endfor

; do a sum for each sector
for sector = 1, nsectors do begin

  ; find boundaries between sectors
  sectorWidth = 2*3.14159 / nsectors
  lowThet = (sector - 1)*sectorWidth
  highThet = sector*sectorWidth

  ; go through all the rings
  for d=0L,(maxdp-1) do begin

    ; find those pixels in the correct ring and sector
    indx = where(theta ge lowThet and theta lt highThet and whichbin eq d)

    ; check to make sure there are pixels in arc
    if (n_elements(indx) eq 0) then begin
      print, 'no data points in ring ' + strcompress(d,/remove_all) + $
          ', sector ' + strcompress(sector,/remove_all)
      return
    endif

    ; collect the pixels in arc and sector
    ringimage=im(indx)
    ringtheta=theta(indx)

    ; blank out pixels more than nsigma away from ring's mean 4 times
    for i = 1, 4 do begin

      me = mean(ringimage)
      sd = stddev(ringimage)

      ; find all pixels within nsigma*sigm from meim
      keep = where(abs(ringimage-me) le nsigma*sd)
      ringimage = ringimage(keep)
      ringtheta = ringtheta(keep)
    endfor

    ; use mean of ring as value of spectrum for the current bin
    binvals[d, sector - 1] = mean(ringimage)

    ; plot ring data points against theta
    if n_elements(plotring) ne 0 then begin
        window,5
        plot,ringtheta,ringimage,psym=2
    endif

    ; add to portion of image representative of sector (for key)
    if n_elements(makeIm) ne 0 then begin
      if d eq 0 then sectInd = indx
      if d ne 0 then sectInd = [sectInd, indx]
    endif
  endfor

  ; declare indices for rest of key
  if n_elements(makeIm) ne 0 then begin

    ; find image of "border": 20 rings past maxdp ring
    for d = maxdp, maxdp + 20 do begin
      indx = where(whichbin eq d and theta ge lowThet and theta lt highThet)
      if d eq maxdp then bordInd = indx
      if d ne maxdp then bordInd = [bordInd, indx]
    endfor

    ; create image of this sector
    sectIm = fltarr(arraydimx,arraydimy)
    sectIm[sectInd] = im[sectInd]

    ; add indices of sector to total list of indices of image
    if sector eq 1 then begin
      totInd = sectInd
    endif else begin
      totInd = [totInd, sectInd]
    endelse

    ; if key has color, define border as the color index assigned to
    ; this sector. otherwise, leave black
    if nsectors gt 1 then begin
      sectIm[bordInd] = 255 - nsectors + sector
    endif else begin
      sectIm[bordInd] = 0
    endelse

    ; define an area to set black in order to show separation between sectors
    r = sqrt(1.*(whichbin+1)*a1dp)
    lineInd = where(r*abs(theta - highThet) lt 1 or r*abs(theta - lowThet) $
        lt 1 or whichbin - maxdp lt 3 and whichbin - maxdp ge 0)
    sectIm[lineInd] = 0

    ; add image of sector to image of key
    newIm = newIm + sectIm
  endif
endfor

; set any pixels that were blanked out to highest considered value

if makeIm ne 0 and toCenter ne 1 then begin

  ; top value used is max in binvals - anything above was blanked out.
  ; remove from image by setting to top value
  topPix = max(binvals)
  tooHigh = where(newIm ge topPix)
  newIm[tooHigh] = topPix

  ; define the color representative of white
  if nsectors gt 1 then begin
    white = 255 - nsectors - 1
  endif else begin
    white = 255
  endelse

  ; scale portion of image considered in ring sum from 0 to <white>,
  ; creating greyscale image surrounded by border key
  bottomPix = where(newIm lt .01)
  newIm[bottomPix] = 0
  newIm[totInd] = (newIm[totInd]/topPix)*white
endif

; consult appropriate variables to determine correct program flow

; this exit goes to end of centering
if final eq 1 then goto, pbocenterfinalback

; this exit goes to middle of centering, to be repeated
if n_elements(center) ne 0 and toCenter eq 1 then goto, pbocenterback

; this exit goes immediately after initial ring sum
toCenter=1
goto, pboback

;*****************************************************************************
;   CENTERING CODE
;*****************************************************************************
centering:

; variables representing a coarse, medium, default, or fine search
coa = 0
med = 1
def = 2
fin = 3

; check keywards to ensure no incompatible choices
if n_elements(coarse) + n_elements(medium) + n_elements(fine) gt 1 then begin
  print, 'Too many centering keywords chosen. Choose a maximum of one ' $
      + 'of /coarse, /medium, and /fine'
  return
endif

; consult keywords to determine ultimate search level
if n_elements(coarse) ne 0 then begin
  search_level = coa
endif else if n_elements(medium) ne 0 then begin
  search_level = med
endif else if n_elements(fine) ne 0 then begin
  search_level = fin
endif else search_level = def

; if /noauto is chosen, path consists only of the desired search level.
; otherwise, path is an array from 0 to the desired search level
if n_elements(noauto) ne 0 then begin
  path = search_level
endif else begin
  path = indgen(search_level + 1)
endelse

; EXAMPLE: for one medium search, path will consist of [ 1 ]
; for a search beginning at coarse and proceeding through fine, path will
; consist of [ 0 1 2 3 ]


; define the central pixel of the first time through the algorithm
origx0 = x0
origy0 = y0

; note that the display window should be opened once
windowOpen = 0

; for each level of the search path, complete the algorithm
for search = 1, n_elements(path) do begin

  ; repeat at each level until condition is satisfied
  done = 0
  while done ne 1 do begin

    ; condition is immediately satisfied for all levels except the first
    ;if search ne 1 then done = 1

    ; define grid/pixel size based on value of path at this search level
    case path[search - 1] of
      coa: begin
             grid = 3.
             pix = 1.
             search_name = 'coarse'
           end
      med: begin
             grid = 5.
             pix = .5
             search_name = 'medium'
           end
      def: begin
             grid = 7.
             pix = .1
             search_name = 'default'
           end
      fin: begin
             grid = 9.
             pix = .05
             search_name = 'fine'
           end
    endcase

    ; declare arrays to hold x and y coordinates of each tested center
    finalxs=fltarr(grid^2)
    finalys=fltarr(grid^2)

    ; declare arrays to hold relevant properties of each tested center
    if nsectors eq 1 then begin

      ; width and height if nsectors is 1
      finalws=fltarr(grid^2)
      finalhs=fltarr(grid^2)
    endif else begin

      ; otherwise, center coordinates for each sector's fit and the
      ; corresponding standard deviation between them
      finalcs=fltarr(grid^2,nsectors)
      finalstdevs=fltarr(grid^2)
    endelse

    ; create starting values of variables so comparison tests below don't
    ; cause errors
    wbest=100000. ; best width
    hbest=0. ; best height
    sbest=100000. ; best st deviation

    ; initialize count, to be incremented for each center
    count = 0

    ; declare variable to provide "starting point" for center grid
    start=pix*((grid - 1)/2)

    ; print beginning of search
    print, ''
    print, 'Beginning ' + search_name + ' search for center:'

    ; create variables for displaying original x and y
    xdisp = strmid(strcompress(x0,/remove_all),0,6)
    ydisp = strmid(strcompress(y0,/remove_all),0,6)

    print, 'Initial center (x0,y0) = (' + xdisp + ',' + ydisp + ')'
    print, strmid(strcompress(grid,/remove_all),0,1) + ' x ' + $
        strmid(strcompress(grid,/remove_all),0,1) + ' grid search'
    print, ' '

    ; loop through each space on grid
    for jj=grid-1,0,-1 do begin
      for ii=0,grid-1 do begin

        ; define x0 and y0 for the ring sum code
        x0=origx0-start+ii*pix
        y0=origy0-start+jj*pix

        ; create variables for displaying current x and y
        xdisp = strmid(strcompress(x0,/remove_all),0,6)
        ydisp = strmid(strcompress(y0,/remove_all),0,6)

        ; go to ring sum, return
        goto,pbocenter
        pbocenterback:

        ; define bounds of gaussian fit (and graphing), only first time
        if ii eq 0 and jj eq grid - 1 then begin

          ; use entire spectrum if xr is omitted
          if n_elements(xr) eq 0 then xr = [0,maxdp]

          ; read into low/high variables
          low = xr[0]
          high = xr[1]

          ; use auto-range algorith if range given is [0,0]
          if low eq 0 and high eq 0 then begin
            fit = gaussfit(bin,binvals[*,0],a,nterms=4)

            ; use center and width from gaussian to create range around peak
            wRange = a[2]
            cRange = a[1]
            low = cRange - 10*wRange
            high = cRange + 10*wRange
          endif

          ; check range values
          if low lt 0 then low = 0
          if high gt maxdp then high = maxdp
          if low ge high then begin
            print, 'Invalid x bounds'
            return
          endif
        endif

        ; create partial bin/binvals variables holding data in provided range
        binInd=indgen(high - low + 1) + low
        partBin=bin[binInd]
        partBinvals=fltarr(n_elements(binInd),nsectors)
        partBinvals[*,*]=binvals[binInd,*]

        ; record current x and y
        finalxs[count] = x0
        finalys[count] = y0

        ; plotting and curve fitting
        ; make window once
        if windowOpen eq 0 then begin
          window, 2, title='Spectral Plot'

          ; now that window is open, change variable accordingly
          windowOpen = 1
        endif

        ; plot first sector in greyscale
        loadct,0,/silent
        plot,partBin,partBinvals[*,0],psym=2,xrange=xr,yrange=yr

        ; for each sector
        for sector = 1,nsectors do begin

          ; find gaussian fit and (xx,yy) containing fit values for
          ; continuous graph (using mask to avoid floating underflow)
          fit = gaussfit(partBin,partBinvals[*,sector-1],a,nterms=4)
          xx = indgen(100*(high - low + 1))*.1 + low
          zz = (xx - a[1])/a[2]
          mask = (zz^2/2 lt 10)
          ee = mask * exp(-(zz^2/2)*mask)
          yy = a[0]*ee + a[3] ;+ a[4]*xx; + a[5]*xx^2

          ; if there are multiple sectors, then
          if nsectors gt 1 then begin

            ; record center locations
            finalcs[count,sector-1] = a[1]
            loadct,39,/silent

            ; plot each sector, its fit, and mark location of its center
            oplot, partBin, partBinvals[*,sector - 1], psym=2,$
                color=colors[sector - 1]
            oplot, xx, yy,color=colors[sector-1]
            oplot,[finalcs[count,sector-1],$
                finalcs[count,sector-1]],[0,100000],$
                color=colors[sector-1]

          ; otherwise, record width and height, then plot fit
          endif else begin
            finalws[count] = a[2]
            finalhs[count] = a[0]
            oplot, xx, yy
          endelse
        endfor

        ; print picture of grid, marking current location with 'X'
        print,''
        print,'(x0,y0) = (' + xdisp + ',' + ydisp + ')'
        print,'-----------------------'
        for jjj = grid - 1, 0, -1 do begin
          print, '     ', format = '(A, $)'
          for iii = 0, grid - 1 do begin
            if (iii eq ii and jjj eq jj) then begin
              print, 'X ', format = '(A, $)'
            endif else begin
              print, 'O ', format = '(A, $)'
            endelse
          endfor
          print, ' '
        endfor

        ; if nsectors is 1, define 'best' center based on smallest width
        if nsectors eq 1 then begin
          if finalws[count] lt wbest then begin
            wbest = finalws[count]
            xbest = x0
            ybest = y0
          endif

        ; otherwise, define best center based on smallest stdv between centers
        endif else begin
          finalstdevs[count] = stddev(finalcs[count,*])
          if (finalstdevs[count] lt sbest) then begin
            sbest=finalstdevs[count]
            xbest=x0
            ybest=y0
          endif
        endelse

        ; increment count
        count = count + 1
      endfor
    endfor

    ; set x0,y0 to best center values
    x0 = xbest
    y0 = ybest

    ; change nsectors to 1 to obtain final width and height info
    numsectors = nsectors
    nsectors = 1

    ; set variable to correct value for appropriate program flow
    final = 1

    ; do final ring sum
    goto, pbocenterfinal
    pbocenterfinalback:

    ; obtain relevant portion of spectrum
    partBinvals[*,*] = binvals[binInd,*]

    ; perform fit, record width and height
    fit = gaussfit(partBin,partBinvals[*,0],b,nterms=4)
    width = b[2]
    height = b[0]

    ; set variable to correct value for appropriate program flow
    final = 0
    nsectors = numsectors

    ; print final information regarding best center
    print,''
    print,''
    xdisp = strmid(strcompress(xbest,/remove_all),0,6)
    ydisp = strmid(strcompress(ybest,/remove_all),0,6)
    print,'Best center (x0,y0) = (' + xdisp + ',' + ydisp + ')
    print,'Width at center = ' + strcompress(width,/remove_all)
    print,'Height at center = ' + strcompress(height,/remove_all)
    print,''

    ; on first time through, only stop while loop when the best center
    ; found is that located at the center of the grid
    if (origx0 eq xbest) and (origy0 eq ybest) then done = 1

    ; set center for next pass to best center
    origx0 = xbest
    origy0 = ybest
  endwhile
endfor

; if desired, rank centers in terms of best to worst
if n_elements(rank) ne 0 then begin

  ; we sort based on appropriate property based on number of sectors
  if nsectors eq 1 then begin
    sorted = sort(finalws)
  endif else begin
    sorted = sort(finalstdevs)
  endelse

  ; display each center and its appropriate statistic, ranked from
  ; most to least desirable
  for i = 0, count - 1 do begin
    xdisp = strmid(strcompress(finalxs[sorted[i]],/remove_all),0,6)
    ydisp = strmid(strcompress(finalys[sorted[i]],/remove_all),0,6)
    if nsectors eq 1 then begin
      wdisp = strcompress(finalws[sorted[i]],/remove_all)
      hdisp = strcompress(finalhs[sorted[i]],/remove_all)
    endif else begin
      stdevdisp = strcompress(finalstdevs[sorted[i]],/remove_all)
    endelse
    print, '(x0,y0) = (' + xdisp + ',' + ydisp + ') -> ', format = '(A, $)'
    if nsectors eq 1 then begin
      print, 'width = ' + wdisp + ', height = ' + hdisp
    endif else begin
      print, 'center std dev = ' + stdevdisp
    endelse
  endfor
  print, ' '
endif

if arg_present(best_center) then best_center = [xbest,ybest]

; exit centering algorithm
goto, centerback

;*****************************************************************************

; here is the end of the program
over:
loadct,0,/silent
END
