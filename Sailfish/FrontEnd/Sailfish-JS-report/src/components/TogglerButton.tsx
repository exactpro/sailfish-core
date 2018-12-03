import {h} from 'preact';
import '../styles/styles.scss';

interface ButtonProps {
    click: Function;
    isToggled: boolean;
    text: string;
    theme?: string;
}

export const TogglerButton = ({click, isToggled, text, theme}: ButtonProps) => {
    const className = ["button-root", (theme || "default"), (isToggled ? "toggled" : "")].join(' ');

    return (<div class={className} onClick={e => click(text)}>
        <p>{text}</p>
    </div>)
}