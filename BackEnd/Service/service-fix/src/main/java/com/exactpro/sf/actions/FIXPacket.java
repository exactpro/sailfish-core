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
package com.exactpro.sf.actions;

import java.util.ArrayList;

import com.exactpro.sf.scriptrunner.ScriptRunException;

public class FIXPacket {

	/**
	 * @param args
	 */
	public int RealLen; // count of real byte
	private ArrayList<String> elements;

	private byte[] messInBytes;

	public FIXPacket( String mVer, String mType )
	{
		this.elements = new ArrayList<String>();
		this.elements.add( "8=" + mVer );
		this.elements.add( "9=?" );
		this.elements.add( "35=" + mType );
		this.messInBytes = new byte[65536];
	}

	public int getCountItems()
	{
		return elements.size();
	}

	// return tag with value (tag=value)
	public String getItemByIndex(int ind)
	{
		return elements.get( ind ).toString();
	}

	private int getFIXLength()
	{
		int allmesslen = 0;
		for (int a = 2; a < this.elements.size(); a++)
		{
		   allmesslen += this.elements.get(a).toString().length();
		   allmesslen += 1;
		}
		return allmesslen;
	}

	private static String getFIXCheckSum(byte[] messarray, int countbytes)
	{
		 int chksum = 0;
		 for (int a = 0; a < countbytes; a++)
		   chksum += messarray[ a ];
		 chksum = chksum % 256;
		 String spriv;
		 spriv = "" + chksum;
		 if ( spriv.length() == 1 )
		   spriv = "00" + spriv;
		 if ( spriv.length() == 2 )
		   spriv = "0" + spriv;
		 return spriv;
	}

	// return in string "as is" without control of length and chksum
	public String getInText()
	{
		String tmpString = "";
		for (int i = 0; i < elements.size(); i++ )
			tmpString += elements.get(i) + "|";
		return tmpString;
	}

	public void setTag( String tagname, String value )
	{
		String tmpString = "";
		for (int i = 0; i < elements.size(); i++ )
		{
			tmpString = elements.get(i).toString();
			if ( tmpString.indexOf( tagname + "=" ) == 0 )
			{
			  elements.set(i, tagname + "=" + value );
			  return;
			}
		}
		elements.add( tagname + "=" + value );
	}

	public void setNewTag( String tagname, String value )
	{
		for (int i = 0; i < elements.size(); i++ )
		{
			String tmpString = elements.get(i).toString();
			if (tmpString.startsWith(tagname + "="))
			{
			  /*if ( i < (elements.size()-1) )
			  {
				elements.add( i+1, tagname + "=" + value );
			  } else*/
			  { elements.add( tagname + "=" + value ); }
			  return;
			}
		}
		elements.add( tagname + "=" + value );
	}

	public boolean delTag( String tagname )
	{
		String tmpString = "";
		for (int i = 0; i < elements.size(); i++ )
		{
			tmpString = elements.get(i).toString();
			if ( tmpString.indexOf( tagname + "=" ) == 0 )
			{
			  elements.remove( i );
			  return true;
			}
		}
		return false;
	}

	public String getTag( String tagname )
	{
		String tmpString = "";
		for (int i = 0; i < elements.size(); i++ )
		{
			tmpString = elements.get(i).toString();
			if ( tmpString.indexOf( tagname + "=" ) == 0 )
			{
			  return tmpString.split("=")[1];
			}
		}
		return "TagNotFound!";
	}

	public void fillPacketFromString( String stringFIXmessage )
	{
		elements.clear();
		String stringmessage[] = null;
		stringmessage = stringFIXmessage.split("\001");
		for (int i = 0; i < stringmessage.length; i++)
		{
  		  elements.add( stringmessage[i] );
		}
	}

	public void fillPacketFromString2( String stringFIXmessage )
	{
		elements.clear();
		String stringmessage[] = null;
		stringmessage = stringFIXmessage.split("!");
		for (int i = 0; i < stringmessage.length; i++)
		{
  		  elements.add( stringmessage[i] );
		}
	}

	// return message in bytes with correct chksum and len
	public byte[] getInBytes()
	{
		try
		  {
			this.delTag("10");
			this.setTag( "9", "" + getFIXLength());

			String[] messarray = elements.toArray(new String[elements.size()]);

	  		int a = 0;
			for (int i = 0; i < messarray.length; i++)
			{
			  byte[] tmp = messarray[ i ].getBytes("US-ASCII");
			  for (int j = 0; j < messarray[ i ].length(); j++)
			  {
				  this.messInBytes[ a ] = tmp[ j ];
				  a++;
			  }
			  this.messInBytes[ a ] = 1;
			  a++;
			}
			this.setTag("10", getFIXCheckSum( this.messInBytes, a ));
			String tmpString;
			tmpString = "10=" + getFIXCheckSum( this.messInBytes, a );

			byte[] tmp = tmpString.getBytes("US-ASCII");
			for (int j = 0; j < tmpString.length(); j++)
			  {
				 this.messInBytes[ a ] = tmp[ j ];
				 a++;
			  }
			this.messInBytes[ a ] = 1;
			this.RealLen = a + 1;
		  }
		  catch (Exception e)
		  {
			 throw new ScriptRunException(e);
		  }
		return messInBytes;
	}

	public byte[] getInDirtyBytes( String dirtyLen, String dirtyChkSum )
	{
		try
		  {
			this.delTag("10");
			if ( dirtyLen != null )
			{
				this.setTag( "9", dirtyLen );
			} else
			{
				this.setTag( "9", "" + getFIXLength());
			}

			String[] messarray = elements.toArray(new String[elements.size()]);

	  		int a = 0;
			for (int i = 0; i < messarray.length; i++)
			{
			  byte[] tmp = messarray[ i ].getBytes("US-ASCII");
			  for (int j = 0; j < messarray[ i ].length(); j++)
			  {
				  this.messInBytes[ a ] = tmp[ j ];
				  a++;
			  }
			  this.messInBytes[ a ] = 1;
			  a++;
			}
			String tmpString = "";
			if ( dirtyChkSum != null )
			{
				// DG: do not set ChkSum(10) to message if DirtyCheckSum=no
				if (false == "no".equalsIgnoreCase(dirtyChkSum))
				{
					this.setTag("10", dirtyChkSum );
					tmpString = "10=" + dirtyChkSum;
				}
			}
			else
			{
				this.setTag("10", getFIXCheckSum( this.messInBytes, a ));
				tmpString = "10=" + getFIXCheckSum( this.messInBytes, a );
			}

			byte[] tmp = tmpString.getBytes("US-ASCII");
			for (int j = 0; j < tmpString.length(); j++)
			  {
				 this.messInBytes[ a ] = tmp[ j ];
				 a++;
			  }
			this.messInBytes[ a ] = 1;
			this.RealLen = a + 1;
		  }
		  catch (Exception e)
		  {
			 throw new ScriptRunException(e);
		  }
		return messInBytes;
	}
}