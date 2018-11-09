import { h, Component } from 'preact';
import { Header } from './Header';

interface LayoutProps {

}

interface LayoutState {

}

export default class BaseLayout extends Component<LayoutProps, LayoutState> {

    constructor(props: LayoutProps) {
        super(props);
        this.state = {

        }
    }

    render({} : LayoutProps, {} : LayoutState) {
        return <Header
            Name="test"
            Time="16.00"
            StartTime="19.00"
            FinishTime="20.00"
            Id="10000"
            Hash="dfjbasdfbjhadsfjbhasd"
            Description="jbhdsafjdsfjkbdsfa"
            Status="FAILED"/>
    }
}