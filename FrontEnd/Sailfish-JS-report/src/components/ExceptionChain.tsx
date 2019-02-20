/******************************************************************************
 * Copyright 2009-2019 Exactpro (Exactpro Systems Limited)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
import {h} from 'preact';
import ExceptionCard from './ExceptionCard';
import Exception from '../models/Exception';
import '../styles/statusPanel.scss';
import { getHashCode } from '../helpers/stringHash';
import { treeToList } from '../helpers/exceptionTreeToListConverter'

interface ExceptionCardProps {
    exception: Exception;
}

export const ExceptionChain = ({exception}: ExceptionCardProps) => {
      
    const rootExceptionCard = (exception == null) ? null :
        <ExceptionCard 
            key={getHashCode(exception.stacktrace + exception.message + exception.class)} 
            exception={exception} 
            drawDivider={false}/>;

    const exceptions = (exception == null) ? null : 
        treeToList(exception.cause).map((ex) => 
            <ExceptionCard 
                key={getHashCode(ex.stacktrace + ex.message + ex.class)} 
                exception={ex} 
                drawDivider={true}/>);

    return (
        <div>
            {rootExceptionCard}
            {exceptions}
        </div>
    );
}