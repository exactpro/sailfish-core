import { h, Component } from "preact";
import "../styles/checkpoint.scss";

interface CheckpointProps {
    name: string;
    count: number;
    isSelected: boolean;
}

export class Checkpoint extends Component<CheckpointProps> {

    render({ name, count, isSelected }: CheckpointProps) {

        const rootClassName = ["checkpoint", (isSelected ? "selected" : "")].join(' ')

        return (
            <div class={rootClassName}>
                <div class="checkpoint-icon" />
                <div class="checkpoint-count">Checkpoint {count}</div>
                <div class="checkpoint-name">{name}</div>
            </div>
        )
    }
}