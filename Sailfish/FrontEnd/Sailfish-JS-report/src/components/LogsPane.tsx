import {h} from 'preact';
import Log from '../models/Log';

interface LogsPaneProps {
    logs: Log[];
}

export const LogsPane = ({logs}: LogsPaneProps) => {
    return (
        <div style={{paddingTop: 60}}>LOGS - SOON...</div>
    );
}