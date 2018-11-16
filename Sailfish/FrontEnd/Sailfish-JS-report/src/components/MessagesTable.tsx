import { h } from 'preact';
import Message from '../models/Message';

interface TableProps {
    messages: Array<Message>;
    selectedActionID: string;
}

export const MessagesTable = ({messages, selectedActionID}: TableProps) => {
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
                    {renderRows(messages, selectedActionID)}
                </tbody>
            </table>
        </div>
    )
}

const renderRows = (messages: Array<Message>, actionId: string) : Array<JSX.Element> => {
    const relatedMessages = messages.filter(message => 
        message.relatedActions.includes(actionId));
    if (!relatedMessages.length) {
        return messages.map(message => getRow(message));
    }

    const renderElements = [];
    let includesPrev = relatedMessages.includes(messages[-1]);
    let includesCurrent = relatedMessages.includes(messages[0]);
    let includesNext ;

    for(let i = 0; i < messages.length; i++) {
        includesNext = relatedMessages.includes(messages[i+1]);
        if (includesCurrent) {
            if (includesPrev) {
                if (includesNext) {
                    renderElements.push(getRow(messages[i], ["selected"]));
                } else {
                    renderElements.push(getRow(messages[i], ["selected", "last"]));
                }
            } else {
                if (includesNext) {
                    renderElements.push(getRow(messages[i], ["selected", "first"]));
                } else {
                    renderElements.push(getRow(messages[i], ["selected", "first", "last"]));
                }
            }
        } else {
            renderElements.push(getRow(messages[i]));
        }

        includesPrev = includesCurrent;
        includesCurrent = includesNext;
    }

    return renderElements;
}

const getRow = ({timestamp, from, to, msgName, contentHumanReadable, status}: Message, atributes = []) : JSX.Element => {
    let className = "messages-table-row " + ( status ? status.toLowerCase() : "");
    atributes.forEach(attribute => className += ` ${attribute}`)

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
