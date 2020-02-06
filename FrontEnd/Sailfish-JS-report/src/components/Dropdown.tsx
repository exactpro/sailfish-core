/******************************************************************************
 * Copyright 2009-2020 Exactpro (Exactpro Systems Limited)
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

import * as React from 'react';
import { createStyleSelector } from '../helpers/styleCreators';
import '../styles/dropdown.scss';

interface DropdownContextValue {
	isOpen: boolean;
	openMenu: () => void;
	closeMenu: () => void;
	disabled: boolean;
}

const DropdownContext = React.createContext<DropdownContextValue | null>(null);

interface DropdownComposition {
	Trigger: React.FC<DropdownTriggerProps>;
	Menu: React.FC<DropdownMenuProps>;
	MenuItem: React.FC<DropdownItemProps>;
}

interface DropdownProps {
	children: React.ReactNode;
	disabled?: boolean;
	className?: string;
}
const Dropdown: React.FC<DropdownProps> & DropdownComposition = ({
	children,
	className = "",
	disabled = false
}: DropdownProps) =>{
	const [isOpen, setIsOpen] = React.useState(false);

	React.useEffect(() => {
		if (isOpen) {
			document.addEventListener('click', closeMenu);

			return () => {
				document.removeEventListener('click', closeMenu)
			}
		}
	}, [isOpen]);

	const closeMenu = () => setIsOpen(false);
	const openMenu = () => {
		if (!disabled) setIsOpen(true)
	};

	const rootClassName = createStyleSelector(
		"dropdown",
		disabled ? "disabled" : "",
		className
	);

	return (
		<div className={rootClassName}>
			<DropdownContext.Provider value={{isOpen, openMenu, closeMenu, disabled}}>
				{children}
			</DropdownContext.Provider>
		</div>
	)
}

interface DropdownTriggerProps {
	children: React.ReactNode;
	className?: string;
}
function DropdownTrigger({ 
	children,
	className = ""
}: DropdownTriggerProps){
	const { openMenu } = React.useContext(DropdownContext);
	const triggerClassName = createStyleSelector(
		"dropdown__trigger",
		className
	);

	return (
		<div 
			className={triggerClassName} 
			onClick={openMenu}
		>
			{children}
		</div>
	);
}

interface DropdownMenuProps {
	children: React.ReactNode;
	className?: string;
}
function DropdownMenu({ 
	children,
	className = ""
}: DropdownMenuProps){
	const { isOpen } = React.useContext(DropdownContext);
	if (!isOpen) return null;

	const menuClassName = createStyleSelector(
		"dropdown__menu",
		className
	);

	return (
		<ul className={menuClassName}>
			{children}
		</ul>
	)
}

interface DropdownItemProps {
	children: React.ReactNode;
	className?: string;
	onClick?: () => void;
}
function DropdownItem({ children, onClick, className }: DropdownItemProps){
	const { closeMenu } = React.useContext(DropdownContext);

	const handleClick = () => {
		if (onClick) onClick();
		closeMenu();
	}
	const menuItemClassName = createStyleSelector(
		"dropdown__menu-item",
		className
	);

	return (
		<li
			className={menuItemClassName}
			onClick={handleClick}>
			{children}
		</li>
	)
}

Dropdown.Trigger = DropdownTrigger;
Dropdown.Menu = DropdownMenu;
Dropdown.MenuItem = DropdownItem;

export default Dropdown;
