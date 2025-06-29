# JMcomicFix

LiquidBounce NextGen shouldn't fall into the same design traps as a decade ago.

[ QQ Group ](https://qm.qq.com/q/hV8aD5lAe6)

如果你不会使用客户端可以看看液体反射官方安装教程  
启动器： https://liquidbounce.net/docs/get-started/installation  
手动安装：https://liquidbounce.net/docs/get-started/manual-installation  
不会英文？右键打开翻译为中文（简体）

LiquidBounce NextGen的Java Chromium嵌入式框架由于较新硬件驱动或者是其他疑难杂症，
部分电脑启动WebUI会有死的渲染器占用，
任务管理器结束所有JCEF进程 → 开关HUD模块→ 在ClickGUI/HudLayoutEditor 界面Ctrl+R，来刷新WebUI。
如果是用.client theme set你得给ClickGUI的Cache和HudLayoutEditor的StartEditor选项开关一下，
不然它俩会因为ThemeManager.kt具体在89至94行的那几行代码一直挂载，它俩的作用就是重载这两界面，
我不知道删掉或者修改会不会导致其他问题就没有动它。Cache这个功能简单来说就是创建一个不会卸载的界面
通过控制界面显示来实现隐藏并且保存当前的状态，JMcomicFix的ClickGUI还在优化中，如果你可以接受掉帧那就开着吧。
完成以上流程还是掉帧占用异常，那就重复上述操作，JCEF 总共CPU占用应该差不多在3%。

简而言之，使用CSS Keyframes的HUD 性能占用异常高。Island使用了很多Keyframes,如果你不嫌掉帧可以开启它。
我已经尽量没用CSS了，其他组件没那么糟糕，在我的13100ES P106主机上，除了Island关闭外，其他HUD全开也不会掉帧，
只有极小的帧生成时间增加，CPU 使用率只增加了 10% 左右，在简单场景下 GPU 只增加了不到 10%，
这意味着你只需要高版本能够稳定在 60FPS的处理器，8GB 的运行内存，GTX750ti及以上的GPU就可以流畅地运行客户端。

在我的E3 1231v3与GT720 Win11_22H2上，1920x1080分辨率 第一次启动客户端没有上面说的问题。  
HUD组件全开CPU占用差不多在0.1-3%，这足以证明并非JMcomicFix的代码存在问题！

