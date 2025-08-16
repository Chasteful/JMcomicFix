import type {ClientPlayerDataEvent} from "../integration/events";
import type {PlayerData} from "../integration/types";
import {Tween} from "svelte/motion";
import {cubicOut} from "svelte/easing";
import {listen} from "../integration/ws";

let playerData: PlayerData | null = {
    position: {x: 0, y: 0, z: 0},
} as PlayerData;
let lastX = 0;
let lastZ = 0;

export const getBPS = new Tween(0, {duration: 150, easing: cubicOut});

export function horizontalSpeed(
    lastPosition: { x: number; z: number },
    currentPosition: { x: number; z: number },
): number {
    const dx = currentPosition.x - lastPosition.x;
    const dz = currentPosition.z - lastPosition.z;
    return Math.sqrt(dx * dx + dz * dz) * 20;
}

export function isValidSpeed(speed: number, maxSpeed: number = 200): boolean {
    return speed <= maxSpeed;
}

listen("clientPlayerData", ((event: ClientPlayerDataEvent) => {
    if (playerData) {
        lastX = playerData.position.x;
        lastZ = playerData.position.z;
    }

    playerData = event.playerData;

    if (playerData) {
        if (lastX !== undefined && lastZ !== undefined) {
            const speed = horizontalSpeed(
                { x: lastX, z: lastZ },
                { x: playerData.position.x, z: playerData.position.z },
            );

            if (isValidSpeed(speed)) {
                getBPS.target = speed;
            }
        }
    }
}));
