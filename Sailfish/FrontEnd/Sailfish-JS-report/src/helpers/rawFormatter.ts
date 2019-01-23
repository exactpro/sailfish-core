const OFFSET_END = 10,
      HEXADECIMAL_END = 50;
 
/**
 * Splits a raw content into 3 lines : offset, hexadecimal representation and human-readable string.
 * @param raw raw content
 * @return {string[]} firts item - offset, second item - hexadecimal representation, third item - human-readable string
 */
export function splitRawContent(raw: string): string[] {
    // first - offset, second - hexadecimal representation, third - human-readable string
    const ret : string[] = ['', '', ''];

    raw.split('\r\n').forEach(row => {
        ret[0] += row.substring(0, OFFSET_END) + '\r\n';
        ret[1] += row.substring(OFFSET_END, HEXADECIMAL_END) + '\r\n';
        ret[2] += row.substring(HEXADECIMAL_END, row.length) + '\r\n';
    })

    return ret;
}