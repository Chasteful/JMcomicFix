export class Interval {
    private timeouts = new Map<string, ReturnType<typeof setTimeout>>();

    set(key: string, callback: () => void, delay: number) {
        this.clear(key);
        const id = setTimeout(() => {
            callback();
            this.timeouts.delete(key);
        }, delay);
        this.timeouts.set(key, id);
    }

    clear(key: string) {
        const id = this.timeouts.get(key);
        if (id !== undefined) {
            clearTimeout(id);
            this.timeouts.delete(key);
        }
    }

    clearAll() {
        this.timeouts.forEach(id => clearTimeout(id));
        this.timeouts.clear();
    }
}
