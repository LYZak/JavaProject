# 导出数据库为MySQL格式
Write-Host "正在查找依赖库..." -ForegroundColor Yellow

# 查找SQLite JDBC驱动
$sqliteJar = "C:\Users\29223\.m2\repository\org\xerial\sqlite-jdbc\3.44.1.0\sqlite-jdbc-3.44.1.0.jar"

# 查找Jackson库
$jacksonCore = "C:\Users\29223\.m2\repository\com\fasterxml\jackson\core\jackson-core\2.16.0\jackson-core-2.16.0.jar"
$jacksonDatabind = "C:\Users\29223\.m2\repository\com\fasterxml\jackson\core\jackson-databind\2.16.0\jackson-databind-2.16.0.jar"
$jacksonAnnotations = "C:\Users\29223\.m2\repository\com\fasterxml\jackson\core\jackson-annotations\2.16.0\jackson-annotations-2.16.0.jar"

# 查找SLF4J
$slf4jApi = Get-ChildItem "$env:USERPROFILE\.m2\repository" -Recurse -Filter "slf4j-api*.jar" -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty FullName
$slf4jNop = Get-ChildItem "$env:USERPROFILE\.m2\repository" -Recurse -Filter "slf4j-nop*.jar" -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty FullName

# 构建classpath
$cp = "target/classes"
$cp += ";$sqliteJar"
if ($slf4jApi) { 
    $cp += ";$slf4jApi"
    Write-Host "找到SLF4J API: $slf4jApi" -ForegroundColor Green
}
if ($slf4jNop) { 
    $cp += ";$slf4jNop"
    Write-Host "找到SLF4J NOP: $slf4jNop" -ForegroundColor Green
}
$cp += ";$jacksonCore"
$cp += ";$jacksonDatabind"
$cp += ";$jacksonAnnotations"

Write-Host "`n正在导出数据库..." -ForegroundColor Yellow
java -cp $cp com.bigcomp.accesscontrol.util.DatabaseExporter

if (Test-Path "db.sql") {
    Write-Host "`n成功！db.sql 文件已生成" -ForegroundColor Green
    Write-Host "文件位置: $(Resolve-Path db.sql)" -ForegroundColor Cyan
    Get-Item db.sql | Select-Object Name, Length, LastWriteTime
} else {
    Write-Host "`n错误: db.sql 文件未生成" -ForegroundColor Red
}

