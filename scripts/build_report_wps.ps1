param([string]$Workspace='C:\Users\48604\Documents\ChatGPT\安卓开发')
$ErrorActionPreference='Stop'
$project=Join-Path $Workspace 'assignment4-card-game'
$assets=Join-Path $project 'report-assets'
$reference=Join-Path $Workspace '实验四+翻牌游戏应用.docx'
$output=Join-Path $Workspace '实验四_翻牌游戏_实验报告_2024110131宋珺熠.docx'
$qa=Join-Path $Workspace 'tmp\assignment4'
$repoUrl='https://github.com/StephenYI43/card-game-2024110131'
[xml]$unit=Get-Content -LiteralPath (Join-Path $assets 'unit-tests.xml') -Raw
[xml]$ui=Get-Content -LiteralPath (Join-Path $assets 'ui-tests.xml') -Raw
if ($unit.testsuite.tests -ne '14' -or $unit.testsuite.failures -ne '0' -or
    $ui.testsuite.tests -ne '7' -or $ui.testsuite.failures -ne '0') { throw 'Tests must pass before report generation' }
$wps=New-Object -ComObject kwps.Application
$wps.Visible=$false
$wps.DisplayAlerts=0
$doc=$wps.Documents.Open($reference,$false,$false)
$doc.SaveAs($output,12)
function Set-Text([int]$Index,[string]$Text) { $doc.Paragraphs.Item($Index).Range.Text=$Text+"`r" }
function Add-Text([string]$Text,[double]$Size=10.5,[bool]$Bold=$false) {
    $start=$doc.Content.End-1
    $r=$doc.Range($start,$start)
    $r.InsertBefore($Text+"`r")
    $r=$doc.Range($start,$start+$Text.Length+1)
    $r.Font.Name='Times New Roman'
    $r.Font.NameFarEast='宋体'
    $r.Font.Size=$Size
    $r.Font.Bold=[int]$Bold
    $r.Font.Color=0
    $r.Font.Underline=0
    $r.ParagraphFormat.Alignment=0
    $r.ParagraphFormat.LeftIndent=0
    $r.ParagraphFormat.RightIndent=0
    $r.ParagraphFormat.FirstLineIndent=0
    $r.ParagraphFormat.SpaceBefore=0
    $r.ParagraphFormat.SpaceAfter=6
    $r.ParagraphFormat.LineSpacingRule=0
    $r.ParagraphFormat.PageBreakBefore=0
    $r.ParagraphFormat.KeepWithNext=0
    $r.ParagraphFormat.KeepTogether=0
    $r.ParagraphFormat.Borders.Enable=0
    $r.ListFormat.RemoveNumbers()
    return $r
}
function Heading([string]$Text) {
    $r=Add-Text $Text 12 $true
    $r.ParagraphFormat.KeepWithNext=-1
    return $r
}
function Page([string]$Text) { $r=Heading $Text; $r.ParagraphFormat.PageBreakBefore=-1 }
function Images([string[]]$Names,[double]$Width) {
    $r=Add-Text '' 10.5
    $r.ParagraphFormat.Alignment=1
    foreach ($name in $Names) {
        $pos=$doc.Content.End-1
        $shape=$doc.InlineShapes.AddPicture((Join-Path $assets $name),$false,$true,$doc.Range($pos,$pos))
        $shape.LockAspectRatio=-1
        $shape.Width=$Width
        $shape.AlternativeText=$name
        $doc.Range($shape.Range.End,$shape.Range.End).InsertAfter(' ')
    }
    $doc.Range($doc.Content.End-1,$doc.Content.End-1).InsertAfter("`r")
}
$find=$doc.Paragraphs.Item(1).Range.Find
$null=$find.Execute('年  月  日',$false,$false,$false,$false,$false,$true,0,$false,'2026年10月16日',2)
$dateRange=$doc.Paragraphs.Item(1).Range
if ($dateRange.Find.Execute('2026年10月16日')) { $dateRange.Font.Size=8 }
Set-Text 2 '计算机科学学院   2024级1班   实验名称：翻牌游戏应用'
Set-Text 3 '姓名：宋珺熠   学号：2024110131   指导老师：李贵洋   实验成绩：_____'
Set-Text 6 ('GitHub网址：'+$repoUrl)
$r=$doc.Paragraphs.Item(36).Range
$r.Text=$r.Text -replace '(?i)gitee','GitHub'
$gradeNote=$doc.Paragraphs.Item(74).Range.Text.Trim()
$doc.Range($doc.Paragraphs.Item(40).Range.Start,$doc.Content.End-1).Delete()
$doc.Paragraphs.Item(39).Range.ParagraphFormat.PageBreakBefore=-1
$doc.Paragraphs.Item(39).Range.ParagraphFormat.KeepWithNext=-1

Heading '1 程序结构与个人信息修改' | Out-Null
Add-Text '本实验完成了 MainActivity、ProfileActivity 和 GameActivity 三个页面。主页面显示姓名、性别、学号、电话、邮箱、爱好和本次运行最高分；信息修改页面支持保存与取消；游戏页面展示16张牌，接受翻牌操作、更新得分并支持重新开局。应用标题为宋珺熠2024110131。' | Out-Null
Add-Text '实验使用 Android Studio 工程、内置 JBR 21、Android SDK 35、Gradle 8.13、AGP 8.13.2 和 Kotlin 2.1.20。在 Android API 35 模拟器上执行了 Gradle 单元测试与连接设备的 Instrumentation UI 测试。以下均为实际运行截图。' | Out-Null
Images @('01-default-main.png','02-profile-editor.png','03-profile-saved.png') 125
Add-Text '图1至图3依次为默认玩家信息、填写修改内容和保存后的主页面。默认性别、电话、邮箱及爱好均未预设。修改测试使用“示例玩家”、电话10086、player@example.com等演示数据，所选性别和爱好仅用于测试，不代表作者真实信息。取消修改后原信息保持不变。' | Out-Null
Add-Text 'PlayerSession 仅在 Application 内存中保存资料和最高分，没有把修改后的信息写入文件。应用进程重新启动后恢复默认资料；应用标题始终保留作者姓名与学号。' | Out-Null

Page '2 翻牌匹配与计分结果'
Add-Text '游戏界面采用 GridLayout 规整排列16张牌，未翻开的牌显示统一牌背，翻开后显示点数与花色，红桃和方块使用红色。匹配成功的牌保持明牌并禁用，控制器根据模型结果同步得分、翻牌次数、匹配数量与反馈提示。' | Out-Null
Images @('04-card-first.png','05-card-matched.png','07-card-mismatch.png') 125
Add-Text '图4为首次翻牌，得分由0变为-1；再次点击同一张未匹配的明牌会将其收起，本次不再扣分。图5为点数相同的一对，奖励4×4=16分，减去两次翻牌各1分后净得14分。图6为点数、花色均不同的一对，合计扣4分，之前的牌翻回牌背，当前牌保持翻开。' | Out-Null
Add-Text '附件规则还包括仅花色相同时奖励1×4=4分，扣除两次翻牌费用后净得2分，已由单元测试验证。若全部牌匹配完成，或剩余牌之间不再有可匹配组合，界面提示本局结束并允许重新开始。' | Out-Null

Page '3 重新开局 最高分与旋转恢复'
Add-Text '重新开始会重新随机发牌，并将当前分数、翻牌次数和匹配状态归零；本次运行中的最高分不会降低。退出游戏后，主页面立即显示新的最高分。横屏使用8列排列，竖屏使用4列排列，页面内容可滚动。' | Out-Null
Images @('06-card-restart.png','09-main-best.png') 124
Add-Text '图7为重新开局后所有牌恢复牌背、当前分数归零；图8为返回主页面后最高分显示14。修改个人信息和重新开始新游戏均不会清空本次运行的最高分。' | Out-Null
Images @('08-game-landscape.png') 378
Add-Text '图9为运行中旋转至横屏的界面。Bundle 保存牌的花色、点数、翻开/匹配标志、得分、翻牌次数和提示资源ID；重建后从快照恢复。UI测试确认旋转到横屏和再转回竖屏时，当前得分-1及已翻开的牌面保持不变。' | Out-Null

Page '4 模型层主要代码'
Add-Text '模型参照 model.zip 中的 Card、Deck 和 CardMatchingGame 实现，并保持原有计分与翻牌规则。Card.match 优先比较点数，再比较花色；Deck 生成52张不重复扑克牌，每次随机抽取并移除一张；CardMatchingGame 管理已选中、已匹配和当前得分。模型不依赖 Android 界面。' | Out-Null
Images @('code-01-model.png') 390
Add-Text '以上为实际源码带行号摘录。新增快照复制、牌数校验与剩余匹配检测，便于旋转恢复和测试；未改变附件的匹配奖励、失配惩罚和翻牌费用。点数和花色在模型中使用整数与枚举，在View层通过字符串资源转换为牌面。' | Out-Null

Page '5 控制器与界面同步代码'
Add-Text 'GameActivity 接收 GameBoardView 返回的卡牌索引，调用模型的 chooseCardAtIndex，记录最高分后统一执行 render。View只根据模型状态绘制牌面与可点击状态，不在按钮中计算分数。重新开局创建新的模型，再同步全部界面。' | Out-Null
Images @('code-02-controller.png') 390
Add-Text '资料修改通过 setResult 返回数据，由 MainActivity 更新 PlayerSession。保存时校验姓名、数字学号、电话和邮箱；取消修改不写入内存资料。界面标题、按钮、提示、得分数字格式和扑克牌符号都来自 res/values/strings.xml，技术键名与测试诊断信息不作为界面文案显示。' | Out-Null
Add-Text '按钮使用原生 Theme.Material.NoActionBar，并显式设置背景和清除背景 tint，避免主题着色覆盖自定义背景。页面使用 ScrollView、LinearLayout、GridLayout、TextView、Button、EditText、RadioGroup 和 CheckBox 等布局与控件。' | Out-Null

Page '6 单元测试与UI测试'
Add-Text '单元测试位于 app/src/test，使用JUnit 4，执行testDebugUnitTest。14项测试全部通过，覆盖点数优先、同花色匹配、不匹配、多牌拒绝、52张牌无重复、非法牌数、翻牌费用、配对得分、匹配后禁用、快照恢复、非法快照、终局判断、重新开局和最高分保留等断言。' | Out-Null
Images @('test-results.png') 405
Add-Text '设备UI测试位于 app/src/androidTest，通过 connectedDebugAndroidTest 在API 35模拟器执行7项 Instrumentation 测试，全部通过。覆盖默认资料、保存/取消修改、非法输入、首次翻牌/收起、点数配对/重新开局/最高分、失配反馈和旋转恢复。图中汇总直接来自Gradle生成的XML结果，原始XML与真实运行截图一起保存在 report-assets。' | Out-Null
Add-Text '为避免随机牌局使UI断言不稳定，测试只读取当前模型来选择同点数或失配组合，然后通过实际按钮执行交互，并检查真实界面状态。单元测试独立验证模型规则，UI测试验证按钮事件、导航和模型到界面的同步。' | Out-Null

Page '四 实验结果的分析与评价'
Heading '1 实验结果' | Out-Null
Add-Text '三个Activity的导航、个人信息显示与修改、保存和取消、输入校验、翻牌交互、匹配禁用、失配处理、得分同步、重新开始及最高分更新均通过验证。屏幕旋转后当前牌局保持不变。最终JUnit测试14项、设备UI测试7项均为0失败；Android Lint为0错误、6个警告。' | Out-Null
Heading '2 问题分析与处理' | Out-Null
Add-Text 'Windows中文工程路径最初导致JUnit无法加载已编译的测试类，报ClassNotFoundException。将新项目放入固定英文目录，并在原工作区保留目录链接后，14项测试正常执行；没有使用容易失效的临时盘符。首次设备测试缺少UTP组件，联网补齐后可从缓存运行。' | Out-Null
Add-Text '运行截图最初在Activity切换或屏幕旋转动画中途截取，出现重影与倾斜，增加截图前的等待后恢复清晰。复测时资料保存断言早于主页面完成结果回调，改为等待界面出现预期值后再断言，避免导航动画造成偶发失败。记分区改为并排布局后，竖屏能显示更多卡牌。' | Out-Null
Add-Text 'Lint警告涉及SDK/工具版本更新建议、备份规则、未使用资源和仅通过代码创建的自定义View构造方法。它们不阻止本次API 35构建与测试；没有通过禁用Lint来跳过检查。其他Android版本与不同品牌真机尚未验证。' | Out-Null
Heading '3 实验体会' | Out-Null
Add-Text '通过本实验，我将卡牌规则独立到模型层，让控制器负责交互与状态更新，界面只显示模型状态。这样可以分别用JUnit验证计分规则、用设备测试验证页面与按钮是否正确联动。随机发牌需要与可重复的测试数据分开，屏幕旋转则需要保存完整牌局，而不只是保存屏幕上的分数。' | Out-Null
Heading '4 项目地址与复现方法' | Out-Null
Add-Text ('GitHub项目地址：'+$repoUrl) | Out-Null
Add-Text 'Android Studio建议打开固定英文目录 C:\Users\48604\Documents\ChatGPT\android-labs\assignment4-project。当前工作区 assignment4-card-game 是指向它的目录链接，只有一份源码。配置JBR和SDK 35，启动API 35模拟器后运行app。' | Out-Null
Add-Text '在工程目录执行 gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:connectedDebugAndroidTest :app:lintDebug。首次运行需要联网获取依赖；缓存齐全后可添加 --offline。测试HTML报告位于 app/build/reports/tests 和 app/build/reports/androidTests，提交报告由WPS接口填写并导出逐页检查。' | Out-Null
Add-Text '参考资料：课程附件 model.zip、翻牌游戏.pdf 和实验四翻牌游戏应用报告模板。' | Out-Null
Add-Text $gradeNote 9 | Out-Null
$doc.Save()
$doc.ExportAsFixedFormat((Join-Path $qa 'report.pdf'),17)
[pscustomobject]@{Output=$output;Pages=$doc.ComputeStatistics(2);Images=$doc.InlineShapes.Count}
$doc.Close(0)
[void][System.Runtime.InteropServices.Marshal]::ReleaseComObject($doc)
[void][System.Runtime.InteropServices.Marshal]::ReleaseComObject($wps)
