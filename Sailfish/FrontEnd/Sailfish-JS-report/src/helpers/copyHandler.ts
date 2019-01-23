export function copyTextToClipboard(str: string) {
    // dirty hack here - we create new invisible element, select it and copy text to the clipboard
    const element = document.createElement('textarea');

    element.value = str;
    element.setAttribute('readonly', '');
    element.style.position = 'absolute';
    element.style.left = '-9000px';

    document.body.appendChild(element);

    // using select API, copy inner text to clipboard
    element.select();

    document.execCommand('copy');
    document.body.removeChild(element);
}