# Encodes the recorded frames into the README demo.
# 1920x1080, 30 fps, H.264 High, 8 Mbps constant (inside the 6–10 Mbps range), BT.709 limited range.
# If OpenOED-vocal.wav exists, the picture is fitted to the narration and muxed as AAC.
param([double]$Seconds = 60, [string]$Voice = 'OpenOED-vocal.wav')
Set-Location $PSScriptRoot
$inv = [Globalization.CultureInfo]::InvariantCulture
$hasVoice = Test-Path $Voice
if ($hasVoice) { $Seconds = [double]::Parse((ffprobe -v error -show_entries format=duration -of csv=p=0 $Voice), $inv) }
if (-not (Test-Path 'raw.webm')) { throw 'raw.webm is missing. Run node record-demo.mjs first.' }
$recorded = [double]::Parse((ffprobe -v error -show_entries format=duration -of csv=p=0 raw.webm), $inv)
$factor = ($Seconds / $recorded).ToString('0.#####', $inv)
$t = $Seconds.ToString('0.###', $inv)
Write-Host ("recorded {0:N1} s -> {1} s (pts x {2}){3}" -f $recorded, $t, $factor, $(if ($hasVoice) { " + voice-over $Voice" } else { '' }))
$audioIn = if ($hasVoice) { @('-i', $Voice) } else { @() }
$audioOut = if ($hasVoice) { @('-map', '0:v:0', '-map', '1:a:0', '-c:a', 'aac', '-b:a', '192k', '-ar', '48000') } else { @() }
ffmpeg -y -hide_banner -loglevel error -i raw.webm @audioIn `
  -vf "setpts=$factor*PTS,fps=30,scale=1920:1080:flags=lanczos:in_range=full:out_range=tv:out_color_matrix=bt709,format=yuv420p" `
  -t $t -c:v libx264 -preset medium -profile:v high -level 4.1 `
  -b:v 8M -minrate 8M -maxrate 8M -bufsize 8M -x264-params "nal-hrd=cbr:force-cfr=1:colorprim=bt709:transfer=bt709:colormatrix=bt709:range=tv" `
  -colorspace bt709 -color_primaries bt709 -color_trc bt709 -color_range tv `
  @audioOut -movflags +faststart openoed-demo.mp4
