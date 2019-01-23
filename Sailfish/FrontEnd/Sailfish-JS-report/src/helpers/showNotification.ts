const NOTIFICATION_TIMEOUT = 1500;

export function showNotification(text: string) {
    const element = document.createElement('div');
    element.className = 'notification';
    element.innerHTML = `<p>${text}</p>`;

    const root = document.getElementById('index');

    root.appendChild(element);

    window.setTimeout(() => {
        root.removeChild(element);
    }, NOTIFICATION_TIMEOUT);
}