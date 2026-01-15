$file = "d:\work\JavaProject\src\main\java\com\bigcomp\accesscontrol\gui\MainWindow.java"
$zh_props = New-Object System.Collections.Generic.List[string]
$en_props = New-Object System.Collections.Generic.List[string]

$content = Get-Content $file -Raw
# Find all occurrences of zh.put("key", ...); or en.put("key", ...);
# This regex looks for (zh|en).put("key", followed by everything until );
$matches = [regex]::Matches($content, '(?ms)^\s*(zh|en)\.put\("([^"]+)",\s*(.*?)\);')

foreach ($m in $matches) {
    $lang = $m.Groups[1].Value
    $key = $m.Groups[2].Value
    $raw_val = $m.Groups[3].Value
    
    # Process raw_val which might be "part1" + "part2" + ...
    # 1. Remove all quotes and plus signs and newlines between them
    # But keep the content inside quotes
    $val = ""
    $val_matches = [regex]::Matches($raw_val, '"(.*?)"')
    foreach ($vm in $val_matches) {
        $val += $vm.Groups[1].Value
    }
    
    # Java strings use \" for quotes. In .properties, we don't necessarily need to escape them unless they are special.
    # But we should keep the \n literal for the ResourceBundle.
    
    if ($lang -eq "zh") { $zh_props.Add("$key=$val") }
    else { $en_props.Add("$key=$val") }
}

[System.IO.File]::WriteAllLines("d:\work\JavaProject\src\main\resources\messages_zh.properties", $zh_props, (New-Object System.Text.UTF8Encoding($false)))
[System.IO.File]::WriteAllLines("d:\work\JavaProject\src\main\resources\messages_en.properties", $en_props, (New-Object System.Text.UTF8Encoding($false)))
