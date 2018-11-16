import {h} from 'preact';
import '../styles/styles.scss';

interface ButtonProps {
    click: Function;
    isToggled: boolean;
    text: string;
}

export const TogglerButton = ({click, isToggled, text}: ButtonProps) => {
    return (<div class={"button-root" + (isToggled ? " toggled" : "")} onClick={e => click(text)}>
        <p>{text}</p>
    </div>)
}