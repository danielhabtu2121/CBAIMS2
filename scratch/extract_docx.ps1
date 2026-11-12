Add-Type -AssemblyName System.IO.Compression.FileSystem

function Extract-DocxText($docxPath, $outPath) {
    Write-Host "Extracting $docxPath to $outPath..."
    $zip = [System.IO.Compression.ZipFile]::OpenRead($docxPath)
    $entry = $zip.GetEntry("word/document.xml")
    if ($entry -ne $null) {
        $stream = $entry.Open()
        $reader = New-Object System.IO.StreamReader($stream)
        $xmlContent = $reader.ReadToEnd()
        $reader.Close()
        $stream.Close()

        $xml = [xml]$xmlContent
        # Extract text by paragraphs
        $nsManager = New-Object System.Xml.XmlNamespaceManager($xml.NameTable)
        $nsManager.AddNamespace("w", "http://schemas.openxmlformats.org/wordprocessingml/2006/main")
        
        $paragraphs = $xml.SelectNodes("//w:p", $nsManager)
        $sb = New-Object System.Text.StringBuilder
        foreach ($p in $paragraphs) {
            $texts = $p.SelectNodes(".//w:t", $nsManager)
            $line = ""
            foreach ($t in $texts) {
                $line += $t.InnerText
            }
            if ($line.Trim().Length -gt 0) {
                [void]$sb.AppendLine($line)
            }
        }
        [System.IO.File]::WriteAllText($outPath, $sb.ToString(), [System.Text.Encoding]::UTF8)
        Write-Host "Done! Saved $($sb.Length) chars."
    }
    $zip.Dispose()
}

$scratch = "c:\Users\danie_mdh4j2\Desktop\cims proj\scratch"

Extract-DocxText "$scratch\Response to ITMD Request 2.docx" "$scratch\Response_to_ITMD_Request_2.txt"
Extract-DocxText "$scratch\CIMS Business Clarification final..docx" "$scratch\CIMS_Business_Clarification_final.txt"
Extract-DocxText "$scratch\Business Requirements for Business Requirements for Collateral Insurance Management System (CIMS)V1 (1).docx" "$scratch\BRD.txt"
