/******************************************************************************
 * Copyright 2009-2018 Exactpro (Exactpro Systems Limited)
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
package com.exactpro.sf.services.ntg;

import com.exactpro.sf.common.util.HexDumper;
import com.exactpro.sf.util.AbstractTest;
import org.apache.mina.core.buffer.IoBuffer;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Ignore //This test need for specific hand testing
public class TestNTGCodecForWebHexDump extends AbstractTest {
    private static final Logger logger = LoggerFactory.getLogger(TestNTGCodecForWebHexDump.class);

	private final String hexString1 =
		"025F0000004400202020202020202020202020202020202020204E41545F53425F3" +
		"10020204D796C6300202020202001454F70002020020032303132313233312D3233" +
		"3A35393A353901F0C3410040420F00701101002001002000000000010100000000";

	private final String hexString2 =
		"028C00000038016D6001004531303837445F3630000000000000" +
		"0000000000000000000000000000000000000000000000000000" +
		"4F31303837445F34340000000000000000000000003800000000" +
		"0000000000000000000000000000000000081DB5010000000000" +
		"00000000000000000040420F00454F700000000100000000";

	@Test
	public void testhexDump1()
	{
		{
			byte[] bytes1 = new byte[hexString1.length() / 2 ];
			Integer messageLength1 = 0;

			for( int i = 0 ; i < hexString1.length() ; i += 2 )
			{
				String hexDigit  = hexString1.substring( i , i + 2 );
				bytes1[i / 2] = (byte) Integer.parseInt( hexDigit, 16 );

				if( i == 10 )
				{
					messageLength1 = Integer.parseInt( hexDigit, 16 );
				}
			}

			logger.info( "messageLength1 = {}", messageLength1  );

			IoBuffer ioBuf = IoBuffer.wrap( bytes1 );
			ioBuf.position( messageLength1 + 5 );

			String hexDumpValue11 = HexDumper.getHexdump(ioBuf.array());
			logger.info( "hexDumpValue11 = \r\n{}", hexDumpValue11  );

			byte[] rawMsg = new byte[ messageLength1 + 5 ];
			System.arraycopy( ioBuf.array(), 0, rawMsg, 0, ioBuf.position() );

			String hexDumpValue12 = HexDumper.getHexdump(ioBuf.array());
			logger.info( "hexDumpValue12 = \r\n{}", hexDumpValue12 );

		}

		{
			byte[] bytes2 = new byte[hexString2.length() / 2 ];
			Integer messageLength2 = 0;

			for( int i = 0 ; i < hexString2.length() ; i += 2 )
			{
				String hexDigit2  = hexString2.substring( i , i + 2 );
				bytes2[i / 2] = (byte) Integer.parseInt( hexDigit2, 16 );

				if( i == 10 )
				{
					messageLength2 = Integer.parseInt( hexDigit2, 16 );
				}
			}
			logger.info( "messageLength2 = {}", messageLength2  );

			IoBuffer ioBuf2 = IoBuffer.wrap( bytes2 );
			ioBuf2.position( messageLength2 + 5 );

			String hexDumpValue21 = HexDumper.getHexdump(ioBuf2.array());
			logger.info( "hexDumpValue21 = \r\n{}", hexDumpValue21  );

			byte[] rawMsg2 = new byte[ messageLength2 + 5 ];
			System.arraycopy( ioBuf2.array(), 0, rawMsg2, 0, ioBuf2.position() );

			String hexDumpValue22 = HexDumper.getHexdump(ioBuf2.array());
			logger.info( "hexDumpValue22 = \r\n{}", hexDumpValue22 );
		}

		{
			byte[] bytes3 = new byte[hexString2.length() / 2 ];
			Integer messageLength3 = 0;

			for( int i = 0 ; i < hexString2.length() ; i += 2 )
			{
				String hexDigit3  = hexString2.substring( i , i + 2 );
				bytes3[i / 2] = (byte) Integer.parseInt( hexDigit3, 16 );

				if( i == 10 )
				{
					messageLength3 = Integer.parseInt( hexDigit3, 16 );
				}
			}
			logger.info( "messageLength3 = {}", messageLength3  );

			IoBuffer ioBuf3 = IoBuffer.wrap( bytes3 );
			ioBuf3.position( messageLength3 + 5 );

			String hexDumpValue31 = HexDumper.getHexdump(ioBuf3.array());
			logger.info( "hexDumpValue31 = \r\n{}", hexDumpValue31  );

			byte[] rawMsg2 = new byte[ messageLength3 + 5 ];
			System.arraycopy( ioBuf3.array(), 0, rawMsg2, 0, ioBuf3.position() );

			String hexDumpValue32 = HexDumper.getHexdump(ioBuf3.array());
			logger.info( "hexDumpValue32 = \r\n{}", hexDumpValue32 );
		}
	}
}