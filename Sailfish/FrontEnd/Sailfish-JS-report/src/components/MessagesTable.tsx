import { h } from 'preact';
import Message from '../models/Message';

interface TableProps {
    messages: Array<Message>;
}

export const MessagesTable = ({messages}: TableProps) => {
    return(
        <div class="messages-table-root">
            <table>
                <thead>
                    <tr>
                        <th class="field">Timestamp</th>
                        <th class="field">MsgName</th>
                        <th class="field">From</th>
                        <th class="field">To</th>
                        <th>Content</th>
                    </tr>
                </thead>
                <tbody>
                    {messages.map(message => renderRow(message))}
                </tbody>
            </table>
        </div>
    )
}

const renderRow = ({timestamp, from, to, msgName, contentHumanReadable, status}: Message) : JSX.Element => {
    const className = "messages-table-row " + ( status ? status.toLowerCase() : "");
    return (
        <tr class={className}>
            <td>{timestamp}</td>
            <td>{msgName}</td>
            <td>{from}</td>
            <td>{to}</td>
            <td>{contentHumanReadable}</td>
        </tr>
    )
}
