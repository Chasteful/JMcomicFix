export class TimeoutManager {
    private timeouts = new Map<string, NodeJS.Timeout>();
  
    set(key: string, callback: () => void, delay: number) {
      this.clear(key);
      this.timeouts.set(key, setTimeout(() => {
        callback();
        this.timeouts.delete(key);
      }, delay));
    }
  
    clear(key: string) {
      if (this.timeouts.has(key)) {
        clearTimeout(this.timeouts.get(key)!);
        this.timeouts.delete(key);
      }
    }
  
    clearAll() {
      this.timeouts.forEach(timeout => clearTimeout(timeout));
      this.timeouts.clear();
    }
  }