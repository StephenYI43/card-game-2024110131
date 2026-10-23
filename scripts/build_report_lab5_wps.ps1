param([string]$Workspace='C:\Users\48604\Documents\ChatGPT\安卓开发')
$ErrorActionPreference='Stop'
$project=Split-Path $PSScriptRoot -Parent
$assets=Join-Path $project 'report-assets\lab5'
$reference=Join-Path $Workspace '实验五+RecyclerView翻牌游戏应用.docx'
$output=Join-Path $Workspace '实验五_RecyclerView翻牌游戏_实验报告_2024110131宋珺熠.docx'
$qa=Join-Path $Workspace 'tmp\assignment5'
$repoUrl='https://github.com/StephenYI43/card-game-2024110131'
if((Get-FileHash -LiteralPath $reference -Algorithm SHA256).Hash -ne '977E7528BF95E7A632F8F2ADC317DE673CB1A12F77401223402C9010D04C962D') { throw 'Reference template changed' }
[xml]$unit=Get-Content -LiteralPath (Join-Path $assets 'unit-tests.xml') -Raw
[xml]$ui=Get-Content -LiteralPath (Join-Path $assets 'ui-tests.xml') -Raw
if($unit.testsuite.tests -ne '23' -or $unit.testsuite.failures -ne '0' -or $ui.testsuite.tests -ne '10' -or $ui.testsuite.failures -ne '0') { throw 'Require passing lab5 tests' }
$pr=Get-Content -LiteralPath (Join-Path $assets 'github-pr1.json') -Raw | ConvertFrom-Json
if($pr.state -ne 'MERGED') { throw 'Code PR must be merged' }
$wps=New-Object -ComObject kwps.Application
$wps.Visible=$false
$wps.DisplayAlerts=0
$doc=$wps.Documents.Open($reference,$false,$true)
$doc.SaveAs($output,12)
function Set-Text([int]$Index,[string]$Text) { $doc.Paragraphs.Item($Index).Range.Text=$Text+"`r" }
function Add-Text([string]$Text,[double]$Size=10.5,[bool]$Bold=$false) {
    $start=$doc.Content.End-1
    $r=$doc.Range($start,$start)
    $r.InsertBefore($Text+"`r")
    $r=$doc.Range($start,$start+$Text.Length+1)
    $r.Font.Name='Times New Roman'; $r.Font.NameFarEast='宋体'
    $r.Font.Size=$Size; $r.Font.Bold=[int]$Bold; $r.Font.Color=0; $r.Font.Underline=0
    $r.ParagraphFormat.Alignment=0; $r.ParagraphFormat.LeftIndent=0; $r.ParagraphFormat.RightIndent=0
    $r.ParagraphFormat.FirstLineIndent=0; $r.ParagraphFormat.SpaceBefore=0; $r.ParagraphFormat.SpaceAfter=6
    $r.ParagraphFormat.LineSpacingRule=0; $r.ParagraphFormat.PageBreakBefore=0
    $r.ParagraphFormat.KeepWithNext=0; $r.ParagraphFormat.KeepTogether=0
    $r.ParagraphFormat.Borders.Enable=0
    $r.ListFormat.RemoveNumbers()
    return $r
}
function Heading([string]$Text) { $r=Add-Text $Text 12 $true; $r.ParagraphFormat.KeepWithNext=-1; return $r }
function Page([string]$Text) { $r=Heading $Text; $r.ParagraphFormat.PageBreakBefore=-1 }
function Images([string[]]$Names,[double]$Width) {
    $r=Add-Text ''
    $r.ParagraphFormat.Alignment=1
    foreach($name in $Names) {
        $pos=$doc.Content.End-1
        $shape=$doc.InlineShapes.AddPicture((Join-Path $assets $name),$false,$true,$doc.Range($pos,$pos))
        $shape.LockAspectRatio=-1; $shape.Width=$Width; $shape.AlternativeText=$name
        $shape.Range.ParagraphFormat.Alignment=1
        $doc.Range($shape.Range.End,$shape.Range.End).InsertAfter(' ')
    }
    $doc.Range($doc.Content.End-1,$doc.Content.End-1).InsertAfter("`r")
}
$find=$doc.Paragraphs.Item(1).Range.Find
$null=$find.Execute('年  月  日',$false,$false,$false,$false,$false,$true,0,$false,'2026年10月23日',2)
$dateRange=$doc.Paragraphs.Item(1).Range
if($dateRange.Find.Execute('2026年10月23日')) { $dateRange.Font.Size=8 }
Set-Text 2 '计算机科学学院  2024级1班   实验名称：RecyclerView翻牌游戏应用'
$doc.Paragraphs.Item(2).Range.Font.Size=9
Set-Text 3 '姓名：宋珺熠   学号：2024110131   指导老师：李贵洋   实验成绩：_____'
$doc.Paragraphs.Item(3).Range.Font.Size=9
Set-Text 6 ('GitHub网址：'+$repoUrl)
$doc.Paragraphs.Item(6).Range.Font.Size=9
$null=$doc.Content.Find.Execute('gitee',$false,$false,$false,$false,$false,$true,0,$false,'GitHub',2)
Set-Text 19 '（1）新建Dev分支，在新分支完成本次作业，提交到与实验四相同的GitHub仓库，保留main和Dev两个分支；'
Set-Text 20 '（2）设置main分支保护，禁止直接push更新；通过GitHub发起PR，记录审核并合并分支；'
$gradeNote=$doc.Paragraphs.Item(71).Range.Text.Trim()
$doc.Range($doc.Paragraphs.Item(37).Range.Start,$doc.Content.End-1).Delete()
$doc.Paragraphs.Item(36).Range.ParagraphFormat.PageBreakBefore=0
$doc.Paragraphs.Item(36).Range.ParagraphFormat.KeepWithNext=-1

Heading '1 程序结构与关于对话框' | Out-Null
Add-Text '本实验在实验四已完成版本上新建独立工程，使用Dev分支实现RecyclerView三牌游戏。程序保留玩家主页、资料修改和游戏三个Activity，新增关于菜单、适配器回调、模型reset和横竖屏新局策略。23项单元测试、10项API 35设备UI测试均通过，功能代码已通过GitHub PR合并。' | Out-Null
Images @('01-default-main.png','03-about-dialog.png') 170
Add-Text '图1为主页面，显示作者姓名与学号、未知个人信息的占位文字和本次运行最高分；图2为点击右上角溢出菜单“关于应用”后出现的对话框，展示作者、学号、版本、MVC说明和完整规则。菜单标题、对话框文字和按钮均从字符串资源读取。' | Out-Null
Add-Text 'MainActivity通过onCreateOptionsMenu加载菜单资源，在onOptionsItemSelected中创建AlertDialog。资料修改沿用实验四的保存、取消和输入校验，修改后的资料及最高分仅保存在Application内存中。测试中的“示例玩家”、10086、player@example.com、性别和爱好均为演示数据，不作为作者真实资料。' | Out-Null

Page '2 RecyclerView与三牌匹配结果'
Add-Text '游戏View改为RecyclerView与GridLayoutManager。竖屏新局由24张不重复卡牌组成，每行4张，列表可滚动。前两张未匹配明牌保持翻开，第三张翻开后统一判定；同点数优先于同花色。成功时三张保持明牌并禁用，失配时前两张收起，保留新翻的第三张。' | Out-Null
Images @('06-board-24.png','09-triple-matched.png','12-card-mismatch.png') 125
Add-Text '图3为24张牌的新局；图4为三张方块匹配，奖励12分，减去三次各1分的翻牌费用后净得9分，匹配数为3/24；图5为失配，三次翻牌共扣3分，再扣失配费用2分，合计-5分。界面同步显示得分、最高分、翻牌次数、匹配数量和反馈。' | Out-Null
Add-Text '三张同点数时Card.match返回6，再乘MATCH_BONUS=4，奖励24分，连续从零翻三张净得21分；三张仅同花色时返回3，奖励12分。点击未匹配的明牌免费收起，不增加翻牌次数。没有可匹配三牌组合时提示结束，避免无法完成的随机尾局。规则和终局均由模型处理。' | Out-Null

Page '3 重新开始与横竖屏牌数'
Images @('10-card-reset.png','11-main-best.png') 121
Add-Text '图6为重新开始：原模型调用reset重新发牌，分数、翻牌次数及匹配状态归零；图7为返回主页面后最高分仍为9。reset不替换模型实例，避免适配器继续持有旧对象。' | Out-Null
Images @('14-landscape-36.png') 390
Add-Text '图8为横屏重新开局后的36张牌，每行9张，通过列表滚动查看其余卡牌。默认竖屏新局24张、每行4张；横屏新局36张、每行9张。旋转时保留正在进行的24或36张牌局，只改变列数，不强制重新发牌；点击重新开始后才按当前方向调整牌数。' | Out-Null
Add-Text 'Bundle保存全部牌的花色、点数、翻开/匹配标志、得分、次数和提示。设备测试确认两张待选明牌与-2分在横竖屏往返后完整保留；横屏重开变36张，回竖屏仍保留36张，竖屏再次重开恢复24张。' | Out-Null

Page '4 适配器与回调主要代码'
Add-Text 'CardAdapter继承RecyclerView.Adapter，重载onCreateViewHolder、onBindViewHolder和getItemCount，创建Button条目并绑定牌面、颜色、背景、描述与禁用状态。点击时读取bindingAdapterPosition并排除NO_POSITION，回调给GameActivity，不在适配器内计分。以下为实际源码带行号摘录。' | Out-Null
Images @('code-adapter.png') 390
Add-Text '适配器对完整卡牌快照执行DiffUtil比较，再派发条目变更，避免每次全量刷新。绑定时始终重置全部可变视觉属性，防止已匹配按钮被复用后带着旧背景或禁用状态。UI测试滚动至第24张再返回第1张，验证隐藏牌和已翻牌状态没有串位。GameActivity负责接收回调、调用模型、更新最高分后执行render。' | Out-Null

Page '5 三牌模型与reset主要代码'
Add-Text '三牌规则的核心修改集中在Card.match和CardMatchingGame.chooseCardAtIndex。已有两张待选牌时才计算匹配分；失配只收起旧的两张牌。保留每次新翻牌扣1分、失配另扣2分、基础分乘4的计分结构，三牌基础奖励分别设计为6和3。' | Out-Null
Images @('code-model.png') 390
Add-Text 'reset先校验牌数并生成新牌，再清空并替换原列表，保证错误参数不会破坏原牌局。新牌数必须为3的倍数且不超过52张；快照校验最多两张待选牌、已匹配数量为3的倍数及卡牌唯一性。模型仍不依赖Android界面，可独立测试。' | Out-Null

Page '6 单元测试与设备UI测试'
Add-Text '本机执行Gradle构建、testDebugUnitTest、connectedDebugAndroidTest和lintDebug。23项JUnit模型测试及10项API 35模拟器Instrumentation UI测试全部通过；Lint为0错误、3个警告。下图根据Gradle原始XML汇总，详细断言记录可在仓库中复核。' | Out-Null
Images @('test-results-lab5.png') 405
Add-Text '模型测试覆盖三点数、三花色、规则优先、错误张数、52张牌唯一性、前两张等待、免费收牌、21/9/-5计分、失配标志、reset清零与24/36牌数、非法reset的原子性、深复制快照、非法快照、终局与最高分。' | Out-Null
Add-Text '设备测试覆盖默认身份、真实菜单点击与关于对话框、资料保存/取消和错误输入、24张牌与列表复用、两张待选牌、三牌匹配/重开/最高分、失配、旋转恢复和横屏36张新局。测试只读随机模型来选择组合，再点击实际按钮和检查界面；前9张中必有三张同花色，避免随机牌局造成不稳定断言。' | Out-Null
Add-Text '原始unit-tests.xml、ui-tests.xml和15张真实运行截图保存在report-assets/lab5。已验证范围为Android 15/API 35模拟器；其他Android版本和品牌真机尚未验证。3个Lint警告为target/compile SDK更新建议以及备份提取规则提示，没有通过禁用Lint跳过检查。' | Out-Null

Page '7 GitHub分支保护与PR合并'
Add-Text '按本人使用GitHub的约定，沿用实验四仓库，在Dev分支提交实验五。main要求PR、Local Android verification提交检查和对话解决，保护对管理员同样生效，并禁止强制推送及删除。实验四历史提交3617f28保留为lab4-complete标签。' | Out-Null
Images @('github-record.png') 405
Add-Text '图中是GitHub真实API记录的摘要。功能PR #1由Dev合并至main，合并提交为e4b4ed36c54a，未使用管理员绕过。合并后保留Dev分支。个人代码自审记录在PR评论中；GitHub不允许作者批准自己的PR，因此外部批准数量设为0，但仍强制PR和成功检查，不伪装成他人审核。' | Out-Null
Add-Text '本次GitHub登录缺少workflow写入权限，无法上传Actions配置。构建、23项单元测试、10项设备UI测试和Lint均在本机实际完成，再把成功状态与原始证据链接登记到对应提交；该检查不是云端CI。仓库中保留分支保护配置JSON和接口记录，便于复核。' | Out-Null
Add-Text ('仓库地址：'+$repoUrl) | Out-Null
Add-Text ('功能PR地址：'+$repoUrl+'/pull/1') | Out-Null
Add-Text ('实验四保留地址：'+$repoUrl+'/tree/lab4-complete') | Out-Null
Add-Text '本地实验五单独位于固定英文路径assignment5-project，实验四仍位于assignment4-project。独立包名com.songjunyi.cardgame.lab5可以与实验四同时安装，工作区APK和实验报告也采用实验五文件名。' | Out-Null

Page '四 实验结果的分析与评价'
Heading '1 实验结果' | Out-Null
Add-Text '本实验完成了RecyclerView 24张牌、适配器与回调、三牌匹配、reset、关于菜单、横竖屏不同新局牌数和状态恢复。View只显示状态，Controller处理交互并调用Model，模型独立计分。个人信息修改与最高分功能保留。构建成功，23项模型测试和10项设备UI测试均为0失败。' | Out-Null
Heading '2 问题分析与解决' | Out-Null
Add-Text '增加RecyclerView后首次离线检查缺少androidx.collection依赖，联网补齐缓存后构建正常。首轮菜单测试使用Instrumentation的旧菜单分发接口，不能定位Toolbar溢出菜单，尽管菜单已经显示；改为在Instrumentation中查找实际菜单条目并点击，再检查对话框中的学号，完整复测后10项设备测试全部通过。' | Out-Null
Add-Text '列表复用容易导致牌面、背景或禁用状态遗留，因此onBindViewHolder每次都重设相关属性。点击回调不捕获过期position，使用bindingAdapterPosition并检查NO_POSITION。采用快照与DiffUtil更新后，全量刷新警告也已消除。' | Out-Null
Add-Text '横竖屏不同新局牌数与“旋转不丢牌局”需要区分：首次进入或重开按方向选择24/36张，普通旋转只改变列数。快照保留两张待选牌，使第三张判定仍可继续，避免只保存分数造成模型与画面不一致。' | Out-Null
Heading '3 实验体会与复现' | Out-Null
Add-Text '通过本实验，我把三牌规则放在模型层，通过适配器回调连接控制器，而没有把计分写进按钮。模型测试可以精确验证奖励与扣分，设备测试补充验证列表复用、菜单、导航和重建。Git分支保护把合并集中到PR中，个人自审记录与独立批准也应如实区分。' | Out-Null
Add-Text 'Android Studio打开 C:\Users\48604\Documents\ChatGPT\android-labs\assignment5-project。安装SDK 35并启动API 35模拟器后运行app。工作区assignment5-recyclerview是指向该目录的链接，不是实验四目录，也不依赖临时盘符。' | Out-Null
Add-Text '复现命令：gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:connectedDebugAndroidTest :app:lintDebug。首次需要联网，缓存齐全后可加--offline。设备测试的本机验证与GitHub提交状态分别记录，报告通过WPS接口填写并导出检查。' | Out-Null
Add-Text '参考资料：课程实验五报告模板；Android Developers的RecyclerView说明及1.4.0发行页；GitHub Docs的分支保护与PR说明。完整链接及测试证据见仓库README。' | Out-Null
Add-Text $gradeNote 9 | Out-Null
$doc.Save()
$doc.ExportAsFixedFormat((Join-Path $qa 'report.pdf'),17)
[pscustomobject]@{Output=$output;Pages=$doc.ComputeStatistics(2);Images=$doc.InlineShapes.Count}
$doc.Close(0)
[void][Runtime.InteropServices.Marshal]::ReleaseComObject($doc)
[void][Runtime.InteropServices.Marshal]::ReleaseComObject($wps)
Copy-Item -LiteralPath $output -Destination (Join-Path $project 'docs')
