<script lang="ts">
    import {fade} from "svelte/transition";

    const tips = [
        {
            title: "京东白条",
            desc1: "许锦良要买 iPhone 16 Pro Max，你买不买？你死也得买",
            desc2: "那买完了京东白条还不上了怎么办？不可能还不上，他连逾期都不是，只是严重警告你同学不能再用你妈的京东白条贷款"
        },
        {
            title: "切换背景",
            desc1: "需要动态背景，但找不到切换背景渲染方式的按钮？试试",
            desc2: "移动光标到左上角客户端图标，左键将会伴随特效，右键为仅切换背景"
        },
        {
            title: "强制退出",
            desc1: "在此界面，因某些原因停留太久需要强制退出？试试",
            desc2: "将你的键盘或是其他输入设备上，所标注Esc的键按下激活，或是点击当前界面中央区域"
        }
    ];

    function wrapTitle(str: string, title: string): string {
        return str.replace(new RegExp(title, "g"), `「${title}」`);
    }

    let currentTip = $state({ title: "", desc1: "", desc2: "" });

    function refreshContent(): void {

        const randomTip = tips[Math.floor(Math.random() * tips.length)];
        currentTip = {
            title: randomTip.title,
            desc1: wrapTitle(randomTip.desc1, randomTip.title),
            desc2: wrapTitle(randomTip.desc2, randomTip.title)
        };
    }

    refreshContent();
</script>
<div
        class="splash-tip"
        role="button"
        tabindex="0"
        onclick={refreshContent}
        onkeydown={(e) => e.key === 'Enter' && refreshContent()}
        transition:fade={{ duration: 600 }}
>
    <h1>{currentTip.title}</h1>
    <p class="desc1">{currentTip.desc1}。</p>
    <p class="desc2">{currentTip.desc2}···</p>
</div>

<style>
    .splash-tip {
        position: fixed;
        left: 50%;
        top: 75%;
        -webkit-font-smoothing: none;
        -moz-osx-font-smoothing: grayscale;
        transform: translate(-50%, -50%);
        display: flex;
        font-family: 'Genshin', serif;
        flex-direction: column;
        align-items: center;
        text-align: center;
        white-space: nowrap;
        color: #C5AF89;
        width: 100%;
        gap: 6px;
        z-index: 1000;
    }

    h1 {

        font-size: 36px;
        margin-bottom: 20px;
    }

    .desc1 {
        font-size: 28px;
        line-height: 1.5;
        margin: 10px 0;

    }

    .desc2 {

        font-size: 28px;
        line-height: 1.5;


    }

    @media screen and (max-width: 1600px) {
        .splash-tip {
            transform: translate(-50%, -50%) scale(0.9);
        }
    }

    @media screen and (max-width: 1366px) {
        .splash-tip {
            transform: translate(-50%, -50%) scale(0.8);
        }
    }

    @media screen and (max-width: 1200px) {
        .splash-tip {
            transform: translate(-50%, -50%) scale(0.7);
        }
    }

    @media screen and (max-height: 1100px) {
        .splash-tip {
            transform: translate(-50%, -50%) scale(0.6);
        }
    }

    @media screen and (max-height: 700px) {
        .splash-tip {
            transform: translate(-50%, -50%) scale(0.5);
        }
    }

</style>
