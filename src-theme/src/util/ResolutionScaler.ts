// ResolutionScaler.ts

export interface ResolutionScalerOptions {
    baseResolution: { width: number; height: number };
    minScale?: number;
}

export class ResolutionScaler {
    private readonly minScale: number;
    private baseResolution: { width: number; height: number };
    private scaleFactor: number = 1;

    constructor(options: ResolutionScalerOptions) {
        this.baseResolution = options.baseResolution;
        this.minScale = options.minScale ?? 0.1337;
    }


    public updateScaleFactor(): void {
        this.scaleFactor = this.calcResolutionCoefficient();
    }

    public getScaleFactor(): number {
        return this.scaleFactor;
    }


    private calcAdjustedResolution(): { width: number; height: number } {
        const w = window.innerWidth;
        const h = window.innerHeight;
        const aspect = Math.sqrt(2.5);
        if (w / h > aspect) {
            return {width: h * aspect, height: h};
        } else {
            return {width: w, height: w / aspect};
        }
    }

    private calcResolutionCoefficient(): number {
        const {width, height} = this.calcAdjustedResolution();
        const wRatio = width / this.baseResolution.width;
        const hRatio = height / this.baseResolution.height;
        return Math.min(1, Math.max(this.minScale, Math.min(wRatio, hRatio)));
    }
}
