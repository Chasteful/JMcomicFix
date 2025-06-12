// ResolutionScaler.ts

export interface ResolutionScalerOptions {
    baseResolution: { width: number; height: number };
    minCoeff?: number;
  }

export class ResolutionScaler {
    private readonly minCoeff:number;
    static subscribe(arg0: (v: any) => void) {
        throw new Error("Method not implemented.");
    }
    private baseResolution: { width: number; height: number };
    private scaleFactor: number = 1;

    constructor(options: ResolutionScalerOptions) {
        this.baseResolution = options.baseResolution;
        this.minCoeff = options.minCoeff ?? 0.1337;
    }


    private calcAdjustedResolution(): { width: number; height: number } {
        const w = window.innerWidth;
        const h = window.innerHeight;
        const aspect = Math.sqrt(2.5);
        if (w / h > aspect) {
            return { width: h * aspect, height: h };
        } else {
            return { width: w, height: w / aspect };
        }
    }


    private calcResolutionCoefficient(): number {
        const { width, height } = this.calcAdjustedResolution();
        const wRatio = width / this.baseResolution.width;
        const hRatio = height / this.baseResolution.height;
        return Math.min(1, Math.max(this.minCoeff, Math.min(wRatio, hRatio)));
    }


    public updateScaleFactor(): void {
        this.scaleFactor = this.calcResolutionCoefficient();
    }


    public getScaleFactor(): number {
        return this.scaleFactor;
    }


    public listenResize(callback: () => void): void {
        window.addEventListener("resize", callback);
    }

    public removeResizeListener(callback: () => void): void {
        window.removeEventListener("resize", callback);
    }
}
