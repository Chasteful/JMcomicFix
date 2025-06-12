/// <reference types="svelte" />
/// <reference types="vite/client" />

export {};

declare global {
    interface CSS {
        paintWorklet: Worklet;
    }
}

interface CSSWithPaint extends CSS {
    paintWorklet: Worklet;
  }
  
  declare const CSS: CSSWithPaint;