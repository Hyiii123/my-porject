param(
    [string]$NacosServer = "http://127.0.0.1:8848/nacos",
    [string]$ConfigDirectory = (Join-Path $PSScriptRoot "")
)

$ErrorActionPreference = "Stop"
$configUri = "$NacosServer/v1/cs/configs"

Get-ChildItem -LiteralPath $ConfigDirectory -Filter "*.yml" -File | ForEach-Object {
    $dataId = $_.Name
    $content = Get-Content -LiteralPath $_.FullName -Raw -Encoding UTF8
    $body = @{
        dataId  = $dataId
        group   = "DEFAULT_GROUP"
        content = $content
        type    = "yaml"
    }

    $result = Invoke-RestMethod -Method Post -Uri $configUri -Body $body -ContentType "application/x-www-form-urlencoded; charset=UTF-8"
    if ($result -ne "true") {
        throw "Nacos 配置导入失败: $dataId, 响应: $result"
    }
    Write-Host "已导入 Nacos 配置: $dataId"
}
