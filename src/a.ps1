Get-ChildItem -Recurse -File -Filter *.java |
  Sort-Object FullName |
  ForEach-Object {
    "===== DOSYA: $($_.FullName) ====="
    Get-Content -LiteralPath $_.FullName -Encoding UTF8
    ""
  } | Set-Content -Path "tum_java_dosyalari.txt" -Encoding UTF8