import { h } from 'preact';
import Action from '../models/Action';
import { ActionCard } from './ActionCard';
import '../styles/action.scss';
import { Component } from 'preact';

interface ListState {
    selectedActionId: string;
}

interface ListProps {
    actions: Array<Action>;
    onSelect: (id: string) => void;
}

export default class ActionsList extends Component<ListProps, ListState> {

    constructor(props: ListProps) {
        super(props);
    }

    itemClickHandler(uuid: string) {
        this.setState({
            selectedActionId: uuid
        });
        this.props.onSelect(uuid);
    }

    render({ actions }: ListProps, { selectedActionId }: ListState) {
        return (
            <div class="actions-list">
                {actions.map(action => {
                    const className = "card-root " + action.status.toLowerCase() + 
                        (action.uuid == selectedActionId ? " selected" : "");
                    return (<div class={className}
                        onClick={e => this.itemClickHandler(action.uuid)}>
                        <ActionCard action={action} />
                    </div>)
                })}
            </div>)
    }
}