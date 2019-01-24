import { h } from 'preact';
import { StatusType } from '../models/Status';
import '../styles/header.scss';
import { ToggleButton } from './ToggleButton';

interface FilterPanelProps {
    actionFilterHandler: (status: StatusType) => void;
    fieldsFilterHandler: (status: StatusType) => void;
    actionsFilters: StatusType[];
    fieldsFilters: StatusType[];
}

export const FilterPanel = ({ actionFilterHandler, fieldsFilterHandler, actionsFilters, fieldsFilters }: FilterPanelProps) => {
    return (
        <div class="header-filter">
            <div class="header-filter-togglers">
                <h5>Actions</h5>
                <ToggleButton text="Passed"
                    isToggled={actionsFilters.includes("PASSED")}
                    click={() => actionFilterHandler("PASSED")}
                    theme="green" />
                <ToggleButton text="Failed"
                    isToggled={actionsFilters.includes("FAILED")}
                    click={() => actionFilterHandler("FAILED")}
                    theme="green" />
                <ToggleButton text="Conditioanlly passed"
                    isToggled={actionsFilters.includes("CONDITIONALLY_PASSED")}
                    click={() => actionFilterHandler("CONDITIONALLY_PASSED")}
                    theme="green" />
            </div>
            <div class="header-filter-togglers">
                <h5>Fields</h5>
                <ToggleButton text="Passed"
                    isToggled={fieldsFilters.includes("PASSED")}
                    click={() => fieldsFilterHandler("PASSED")}
                    theme="green" />
                <ToggleButton text="Failed"
                    isToggled={fieldsFilters.includes("FAILED")}
                    click={() => fieldsFilterHandler("FAILED")}
                    theme="green" />
                <ToggleButton text="Conditioanlly passed"
                    isToggled={fieldsFilters.includes("CONDITIONALLY_PASSED")}
                    click={() => fieldsFilterHandler("CONDITIONALLY_PASSED")}
                    theme="green" />
                <ToggleButton text="N/A"
                    isToggled={fieldsFilters.includes("NA")}
                    click={() => fieldsFilterHandler("NA")}
                    theme="green" />
            </div>
            <div class="header-filter-togglers">
                <h5>Messages</h5>
                <ToggleButton text="Checkpoints"
                    isToggled={true}
                    click={() => {}}
                    theme="green" />
            </div>
        </div>
    )
}