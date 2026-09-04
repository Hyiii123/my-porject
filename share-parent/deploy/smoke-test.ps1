[CmdletBinding()]
param(
    [string]$BaseUrl = 'http://127.0.0.1:8080',
    [string]$AdminUsername = 'admin',
    [string]$AdminPassword = 'admin123',
    [string]$StudentUsername = 'student_zhang',
    [string]$StudentPassword = 'admin123',
    [switch]$IncludeWriteFlow
)

$ErrorActionPreference = 'Stop'
$BaseUrl = $BaseUrl.TrimEnd('/')
$script:Passed = 0

function Convert-ApiJson {
    param([Parameter(Mandatory = $true)][string]$Json)

    # Windows PowerShell 5.1 的 ConvertFrom-Json 对 username/userName 这类
    # 大小写不同但语义重复的旧字段会报错，JavaScriptSerializer 可以兼容读取。
    try {
        Add-Type -AssemblyName System.Web.Extensions -ErrorAction Stop
        $serializer = New-Object System.Web.Script.Serialization.JavaScriptSerializer
        return $serializer.DeserializeObject($Json)
    }
    catch {
        return $Json | ConvertFrom-Json
    }
}

function Invoke-Api {
    param(
        [Parameter(Mandatory = $true)][ValidateSet('GET', 'POST', 'PUT', 'DELETE')][string]$Method,
        [Parameter(Mandatory = $true)][string]$Path,
        [object]$Body,
        [string]$Token,
        [string]$Name = $Path
    )

    $headers = @{ Accept = 'application/json' }
    if (-not [string]::IsNullOrWhiteSpace($Token)) {
        $headers.Authorization = "Bearer $Token"
    }

    $request = @{
        Method    = $Method
        Uri       = "$BaseUrl$Path"
        Headers   = $headers
        TimeoutSec = 30
    }
    if ($null -ne $Body) {
        $request.ContentType = 'application/json; charset=utf-8'
        $request.Body = $Body | ConvertTo-Json -Depth 20 -Compress
    }

    try {
        $result = Invoke-RestMethod @request
        # 旧用户兼容接口在部分底座版本中把 JSON 作为字符串返回，统一解码为对象。
        if ($result -is [string] -and $result.TrimStart().StartsWith('{')) {
            $result = Convert-ApiJson -Json $result
        }
        if ($result -is [array] -and $result.Count -eq 1) {
            return $result[0]
        }
        return $result
    }
    catch {
        $detail = $_.ErrorDetails.Message
        if ([string]::IsNullOrWhiteSpace($detail)) {
            $detail = $_.Exception.Message
        }
        throw "$Name 请求失败：$detail"
    }
}

function Assert-Success {
    param(
        [object]$Response,
        [Parameter(Mandatory = $true)][string]$Name,
        [switch]$AllowNullData
    )

    # 某些 PowerShell 版本会把单元素 JSON 集合在函数返回时展开，统一还原后再取业务字段。
    if ($Response -is [array] -and $Response.Count -eq 1) {
        $Response = $Response[0]
    }
    if ($null -eq $Response -or [int]$Response.code -ne 200) {
        $code = if ($null -eq $Response) { '<empty>' } else { $Response.code }
        $msg = if ($null -eq $Response) { '' } else { $Response.msg }
        throw "$Name 业务失败：code=$code $msg"
    }
    # 兼容若依 TableDataInfo（rows）和分页 DTO（list），避免对 Hashtable/PSCustomObject
    # 调用不同签名的 Contains 方法。
    $hasRows = ($null -ne $Response.rows) -or ($null -ne $Response.list)
    if (-not $AllowNullData -and $null -eq $Response.data -and -not $hasRows) {
        throw "$Name 返回 data/rows 为空"
    }
    $script:Passed++
    Write-Host "[PASS] $Name"
    return $Response
}

function Get-Rows {
    param([object]$Data)

    if ($null -eq $Data) { return @() }
    if ($Data -is [System.Collections.IDictionary]) {
        if ($Data.Contains('rows')) { return @($Data['rows']) }
        if ($Data.Contains('list')) { return @($Data['list']) }
    }
    $properties = @($Data.PSObject.Properties.Name)
    if ($properties -contains 'rows') { return @($Data.rows) }
    if ($properties -contains 'list') { return @($Data.list) }
    if ($Data -is [System.Collections.IEnumerable] -and $Data -isnot [string]) {
        return @($Data)
    }
    return @($Data)
}

function Assert-Count {
    param(
        [object]$Data,
        [int]$Minimum,
        [Parameter(Mandatory = $true)][string]$Name
    )
    $count = @(Get-Rows $Data).Count
    if ($count -lt $Minimum) {
        throw "$Name 数据量不足：实际 $count，至少需要 $Minimum"
    }
    $script:Passed++
    Write-Host "[PASS] $Name（$count 条）"
}

function Login {
    param(
        [Parameter(Mandatory = $true)][string]$Username,
        [Parameter(Mandatory = $true)][string]$Password,
        [Parameter(Mandatory = $true)][string]$Name
    )
    $response = Invoke-Api -Method POST -Path '/auth/login' -Body @{ username = $Username; password = $Password } -Name $Name
    Assert-Success $response $Name | Out-Null
    $token = [string]$response.data.access_token
    if ([string]::IsNullOrWhiteSpace($token)) {
        $token = [string]$response.data
    }
    if ([string]::IsNullOrWhiteSpace($token)) {
        throw "$Name 未返回 access_token"
    }
    return $token
}

Write-Host "开始检查网关：$BaseUrl"
$health = Invoke-RestMethod -Method GET -Uri "$BaseUrl/actuator/health" -TimeoutSec 30
if ([string]$health.status -ne 'UP') {
    throw "网关健康检查失败：$($health | ConvertTo-Json -Compress)"
}
$script:Passed++
Write-Host '[PASS] 网关健康检查'

$adminToken = Login -Username $AdminUsername -Password $AdminPassword -Name '管理员登录'
$studentToken = Login -Username $StudentUsername -Password $StudentPassword -Name '学员登录'

$categories = Invoke-Api -Method GET -Path '/cs/categorys/all' -Token $studentToken -Name '课程分类查询'
Assert-Success $categories '课程分类查询' | Out-Null
Assert-Count $categories.data 10 '课程分类'

$courses = Invoke-Api -Method GET -Path '/ss/courses/portal?pageNo=1&pageSize=10' -Token $studentToken -Name '课程门户查询'
Assert-Success $courses '课程门户查询' | Out-Null
Assert-Count $courses.data 1 '课程门户'

$catalogs = Invoke-Api -Method GET -Path '/cs/courses/1/catalogs' -Token $studentToken -Name '课程目录查询'
Assert-Success $catalogs '课程目录查询' | Out-Null
Assert-Count $catalogs.data 1 '课程目录'

$studentInfo = Invoke-Api -Method GET -Path '/us/users/me' -Token $studentToken -Name '学员资料查询'
Assert-Success $studentInfo '学员资料查询' | Out-Null

$learningPage = Invoke-Api -Method GET -Path '/ls/lessons/page?pageNo=1&pageSize=20' -Token $studentToken -Name '学习课程分页查询'
Assert-Success $learningPage '学习课程分页查询' | Out-Null
Assert-Count $learningPage.data 1 '学习课程'

$learningLog = Invoke-Api -Method GET -Path '/ls/learning-records/lessons/10002' -Token $studentToken -Name '小节学习记录查询'
Assert-Success $learningLog '小节学习记录查询' -AllowNullData | Out-Null
if ($null -eq $learningLog.data -or [int64]$learningLog.data.catalogId -ne 10002) {
    throw "小节学习记录未按 catalog_id 返回：$($learningLog | ConvertTo-Json -Depth 10 -Compress)"
}
$script:Passed++
Write-Host '[PASS] 小节记录与目录编号一致'

$coupons = Invoke-Api -Method GET -Path '/prs/coupons/list' -Token $studentToken -Name '可领取优惠券查询'
Assert-Success $coupons '可领取优惠券查询' | Out-Null
Assert-Count $coupons.data 1 '可领取优惠券'

$cartPage = Invoke-Api -Method GET -Path '/ts/carts' -Token $studentToken -Name '购物车查询'
Assert-Success $cartPage '购物车查询' | Out-Null

$orders = Invoke-Api -Method GET -Path '/ts/orders/page?pageNo=1&pageSize=20' -Token $studentToken -Name '用户订单查询'
Assert-Success $orders '用户订单查询' | Out-Null
Assert-Count $orders.data 1 '用户订单'

$channels = Invoke-Api -Method GET -Path '/ts/pay/channels' -Token $studentToken -Name '支付渠道查询'
Assert-Success $channels '支付渠道查询' | Out-Null
Assert-Count $channels.data 2 '支付渠道'

$media = Invoke-Api -Method GET -Path '/ms/medias?pageNo=1&pageSize=10' -Token $adminToken -Name '媒资分页查询'
Assert-Success $media '媒资分页查询' | Out-Null

$adminCodes = Invoke-Api -Method GET -Path '/prs/codes/page?pageNo=1&pageSize=20' -Token $adminToken -Name '管理员兑换码查询'
Assert-Success $adminCodes '管理员兑换码查询' | Out-Null
Assert-Count $adminCodes.data 1 '兑换码'

$adminOrders = Invoke-Api -Method GET -Path '/ts/order-details/page?pageNo=1&pageSize=20' -Token $adminToken -Name '管理员订单明细查询'
Assert-Success $adminOrders '管理员订单明细查询' | Out-Null
Assert-Count $adminOrders.data 1 '管理员订单明细'

$refunds = Invoke-Api -Method GET -Path '/ts/refund-apply/page?pageNo=1&pageSize=20' -Token $adminToken -Name '管理员退款列表查询'
Assert-Success $refunds '管理员退款列表查询' | Out-Null
Assert-Count $refunds.data 1 '退款申请'

$educationStatistics = Invoke-Api -Method GET -Path '/ls/admin/statistics/overview' -Token $adminToken -Name '教育统计查询'
Assert-Success $educationStatistics '教育统计查询' | Out-Null

$tradeStatistics = Invoke-Api -Method GET -Path '/prs/admin/statistics/overview' -Token $adminToken -Name '交易统计查询'
Assert-Success $tradeStatistics '交易统计查询' | Out-Null

$knowledge = Invoke-Api -Method GET -Path '/customer/admin/knowledge/list?pageNo=1&pageSize=20' -Token $adminToken -Name '客服知识库查询'
Assert-Success $knowledge '客服知识库查询' | Out-Null
Assert-Count $knowledge 1 '客服知识库'

$faq = Invoke-Api -Method GET -Path '/customer/admin/faq/list?pageNo=1&pageSize=20' -Token $adminToken -Name '客服常见问题查询'
Assert-Success $faq '客服常见问题查询' | Out-Null
Assert-Count $faq 1 '客服常见问题'

$customerStatistics = Invoke-Api -Method GET -Path '/customer/admin/statistics/overview' -Token $adminToken -Name '客服统计查询'
Assert-Success $customerStatistics '客服统计查询' | Out-Null

if ($IncludeWriteFlow) {
    Write-Host '开始执行可写链路（会产生演示订单和学习记录）'

    $customerSession = Invoke-Api -Method POST -Path '/customer/session' -Token $studentToken -Body @{
        userName = '冒烟测试学员'
    } -Name '创建 AI 客服会话'
    Assert-Success $customerSession '创建 AI 客服会话' | Out-Null
    $customerSessionId = [string]$customerSession.data.id
    if ([string]::IsNullOrWhiteSpace($customerSessionId)) {
        throw '客服会话创建后未返回会话编号'
    }

    # 不提供 API Key，验证第三方 AI 不可用时仍会通过本地 FAQ/知识库完成降级回复。
    $customerChat = Invoke-Api -Method POST -Path "/customer/session/$customerSessionId/messages" -Token $studentToken -Body @{
        content = '如何重置密码？'
    } -Name '客服知识库降级回复'
    Assert-Success $customerChat '客服知识库降级回复' | Out-Null
    if ($null -eq $customerChat.data.message -or [string]::IsNullOrWhiteSpace([string]$customerChat.data.message.content)) {
        throw '客服消息接口未返回 AI/知识库回复'
    }
    if (@($customerChat.data.session.messages).Count -lt 3) {
        throw '客服会话快照未包含欢迎语、用户问题和客服回复'
    }
    $script:Passed++
    Write-Host '[PASS] 客服会话快照包含完整问答链路'

    $customerMessages = Invoke-Api -Method GET -Path "/customer/session/$customerSessionId/messages" -Token $studentToken -Name '客服消息历史查询'
    Assert-Success $customerMessages '客服消息历史查询' | Out-Null
    Assert-Count $customerMessages.data 3 '客服消息历史'

    $customerEvaluation = Invoke-Api -Method POST -Path "/customer/session/$customerSessionId/evaluation" -Token $studentToken -Body @{
        score = 5
        tags = @('回答及时')
        comment = '冒烟测试评价'
    } -Name '提交客服服务评价'
    Assert-Success $customerEvaluation '提交客服服务评价' | Out-Null

    $closedCustomerSession = Invoke-Api -Method GET -Path "/customer/session/$customerSessionId" -Token $studentToken -Name '评价后客服会话查询'
    Assert-Success $closedCustomerSession '评价后客服会话查询' | Out-Null
    if ([int]$closedCustomerSession.data.status -ne 3) {
        throw "提交评价后客服会话未关闭：$($closedCustomerSession | ConvertTo-Json -Depth 10 -Compress)"
    }
    $script:Passed++
    Write-Host '[PASS] 提交评价后客服会话已关闭'

    $adminCustomerSessions = Invoke-Api -Method GET -Path '/customer/admin/sessions/list?pageNum=1&pageSize=20' -Token $adminToken -Name '管理端客服会话查询'
    Assert-Success $adminCustomerSessions '管理端客服会话查询' | Out-Null
    Assert-Count $adminCustomerSessions 1 '管理端客服会话'

    $adminCustomerMessages = Invoke-Api -Method GET -Path "/customer/admin/sessions/$customerSessionId/messages" -Token $adminToken -Name '管理端客服消息查询'
    Assert-Success $adminCustomerMessages '管理端客服消息查询' | Out-Null
    Assert-Count $adminCustomerMessages.data 3 '管理端客服消息'

    $enrollment = Invoke-Api -Method POST -Path '/ls/internal/enrollments/20' -Token $studentToken -Body @{} -Name '课程报名幂等创建'
    Assert-Success $enrollment '课程报名幂等创建' | Out-Null

    $savedLog = Invoke-Api -Method POST -Path '/ls/learning-records' -Token $studentToken -Body @{
        courseId = 1
        catalogId = 10002
        progressPercent = 100
        progressSeconds = 2730
        completedLessons = 1
    } -Name '小节学习记录保存'
    Assert-Success $savedLog '小节学习记录保存' | Out-Null

    $restarted = Invoke-Api -Method PUT -Path '/ls/lessons/1/restart' -Token $studentToken -Body @{} -Name '课程重新学习'
    Assert-Success $restarted '课程重新学习' | Out-Null
    if ($null -eq $restarted.data -or [decimal]$restarted.data.progressPercent -ne 0) {
        throw "课程重新学习后进度未归零：$($restarted | ConvertTo-Json -Depth 10 -Compress)"
    }
    $script:Passed++
    Write-Host '[PASS] 重新学习后进度已归零'

    $exchanged = Invoke-Api -Method POST -Path '/prs/user-coupons/OPEN-11-A003/exchange' -Token $studentToken -Body @{} -Name '兑换码兑换'
    Assert-Success $exchanged '兑换码兑换' | Out-Null

    $cart = Invoke-Api -Method POST -Path '/ts/carts' -Token $studentToken -Body @{ courseId = 20 } -Name '加入购物车'
    Assert-Success $cart '加入购物车' | Out-Null
    $cartId = [string]$cart.data.id

    $preOrder = Invoke-Api -Method GET -Path "/ts/orders/prePlaceOrder?ids=$cartId" -Token $studentToken -Name '订单预结算'
    Assert-Success $preOrder '订单预结算' | Out-Null
    Assert-Count $preOrder.data.items 1 '预结算商品'

    $order = Invoke-Api -Method POST -Path '/ts/orders/placeOrder' -Token $studentToken -Body @{ cartIds = @([int64]$cartId) } -Name '购物车创建订单'
    Assert-Success $order '购物车创建订单' | Out-Null
    $orderId = [string]$order.data.id

    $payment = Invoke-Api -Method POST -Path '/ts/pay/order' -Token $studentToken -Body @{ orderId = [int64]$orderId; channel = 'wechat' } -Name '创建演示支付单'
    Assert-Success $payment '创建演示支付单' | Out-Null

    $paid = Invoke-Api -Method POST -Path "/ts/pay/order/$orderId/demo-success" -Token $studentToken -Body @{} -Name '演示支付成功'
    Assert-Success $paid '演示支付成功' | Out-Null

    $paymentState = Invoke-Api -Method GET -Path "/ts/orders/$orderId/status" -Token $studentToken -Name '支付状态查询'
    Assert-Success $paymentState '支付状态查询' | Out-Null
    if ([int]$paymentState.data.paymentStatus -ne 1) {
        throw "支付状态未变为成功：$($paymentState | ConvertTo-Json -Compress)"
    }
    $script:Passed++
    Write-Host '[PASS] 支付后订单状态为成功'

    $enrolledAfterPay = Invoke-Api -Method GET -Path '/ls/lessons/20' -Token $studentToken -Name '支付后学习权限查询'
    Assert-Success $enrolledAfterPay '支付后学习权限查询' | Out-Null
    if ($null -eq $enrolledAfterPay.data) {
        throw '支付成功后没有创建课程学习记录'
    }
    $script:Passed++
    Write-Host '[PASS] 支付成功后已创建学习记录'
}

Write-Host "冒烟测试完成：$script:Passed 项通过。"
