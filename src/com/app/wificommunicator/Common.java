/**
 * 
 */
package com.app.wificommunicator;

/**
 * @author diete_000
 *
 */
import java.util.Arrays;

public class Common {

	static int crc16Table[] = new int[256];
	
	static int PROTOCOL_FLAG_BYTE = 0x7E; 
	static int PROTOCOL_CTRL_ESC = 0x7D; 
	static int PROTOCOL_XOR_CHAR = 0x20; 
	
	static void MakeCrc16Table(){
		
		int i, bitCtr, crc;
		int len = 256;

        for (i = 0; i < len; i++)
        {
            crc = i << 8;
            for (bitCtr = 0; bitCtr < 8; bitCtr++)
            {
                crc = (crc << 1) ^ ((crc & 0x8000) != 0 ? 0x1021 : 0);
            }
            crc = crc & 0xFFFF;
            crc16Table[i] = crc;
        }
	}
	
    public static boolean IsFcsValid(byte[] fcs, byte[] data)
    {
        int crc = 0;
        MakeCrc16Table();
        for (int i = 0; i < data.length; i++)
        {
            crc = crc16Table[(crc >> 8) & 0xff] ^ (crc << 8) ^ unsignedToBytes(data[i]);
            crc = crc & 0xffff;
        }

        int mask = 0xff;
        byte lsb = (byte)(crc & mask);
        byte msb = (byte)((crc >> 8) & mask);

        if (fcs[0] == lsb && fcs[1] == msb)
            return true;
        else
            return false;
    }

    public static byte[] GetFcs(byte[] data)
    {
        int crc = 0;
        MakeCrc16Table();
        for (int i = 0; i < data.length; i++)
        {
            crc = crc16Table[(crc >> 8) & 0xff] ^ (crc << 8) ^ unsignedToBytes(data[i]);
            crc = crc & 0xffff;
        }

        int mask = 0xff;
        byte lsb = (byte)(crc & mask);
        byte msb = (byte)((crc >> 8) & mask);

        return new byte[] {lsb, msb };
    }
    
/*    public static int find(byte[] source, byte[] match) {
        // sanity checks
        if(source == null || match == null)
          return -1;
        if(source.length == 0 || match.length == 0)
          return -1;
        int ret = -1;
        int spos = 0;
        int mpos = 0;
        byte m = match[mpos];
        for( ; spos < source.length; spos++ ) {
          if(m == source[spos]) {
            // starting match
            if(mpos == 0)
              ret = spos;
            // finishing match
            else if(mpos == match.length - 1)
              return ret;
            mpos++;
            m = match[mpos];
          }
          else {
            ret = -1;
            mpos = 0;
            m = match[mpos];
          }
        }
        return ret;
      }*/
    
  public static int indexOf(byte[] data, byte[] pattern) {
        int[] failure = computeFailure(pattern);

        int j = 0;
        if (data.length == 0) return -1;

        for (int i = 0; i < data.length; i++) {
            while (j > 0 && pattern[j] != data[i]) {
                j = failure[j - 1];
            }
            if (pattern[j] == data[i]) { j++; }
            if (j == pattern.length) {
                return i - pattern.length + 1;
            }
        }
        return -1;
    }

    /**
     * Computes the failure function using a boot-strapping process,
     * where the pattern is matched against itself.
     */
    public static int[] computeFailure(byte[] pattern) {
        int[] failure = new int[pattern.length];

        int j = 0;
        for (int i = 1; i < pattern.length; i++) {
            while (j > 0 && pattern[j] != pattern[i]) {
                j = failure[j - 1];
            }
            if (pattern[j] == pattern[i]) {
                j++;
            }
            failure[i] = j;
        }

        return failure;
    }
    
    
    public static int IndexOf(byte[] arrayToSearchThrough, byte[] patternToFind, int intStart)
    {
        if (patternToFind.length > arrayToSearchThrough.length)
            return -1;
        for (int i = intStart; i < arrayToSearchThrough.length - patternToFind.length; i++)
        {
            boolean found = true;
            for (int j = 0; j < patternToFind.length; j++)
            {
                if (arrayToSearchThrough[i + j] != patternToFind[j])
                {
                    found = false;
                    break;
                }
            }
            if (found)
            {
                return i;
            }
        }
        return -1;
    }
    public static byte[] getRealData(byte[] data)
    {
    	byte[] realData = new byte[0];
    	byte[] searchBytes = new byte[] {(byte)0x7E, (byte)0x87};
    	byte[] endByte = new byte[] {(byte)0x7E};

    	int startIndex = indexOf(data, searchBytes);
    	if(startIndex == -1)
    		return null;
    	
    	int endIndex =  IndexOf(data, endByte, startIndex + 2) + 1;
    	if (endIndex <= 0)
    	{
    		return null;
    	}

    	realData = Arrays.copyOfRange(data, startIndex, endIndex);
    	return realData;
    }
    
    public static  byte[] Depacketize(byte[] data )
    {
    	byte[] depacketizedData = new byte[0];
        // Verify that first and last bytes match header
    	
        int len = data.length;
        if (data[0] != PROTOCOL_FLAG_BYTE ) //|| data[len - 1] != PROTOCOL_FLAG_BYTE)
        {
            return null;
        }

        // look for Control Escape characters 
        //
        int numEscChars = 0;
        for (int i = 1; i < len-1; i++)  //skip first and last bytes because those are the header bytes
        {
            if (data[i] == PROTOCOL_CTRL_ESC)
                numEscChars++;
        }

        // verify whether data is large enough
        //
        if ((len - numEscChars - 2 - 2) < 2)
        {
            return null;
        }

        //contains only the data, so we remove the 2 header bytes, and the FCS bytes
        depacketizedData = new byte[len - numEscChars - 2 - 2];

        int idx = 0;
        int idx2 = 0;
        byte[] fcs = new byte[2];

        for (int i = 1; i < len-1; i++)
        {
            if (idx < depacketizedData.length) // Store data
            {
                if (data[i] == PROTOCOL_CTRL_ESC)
                {
//                    depacketizedData[idx] = (byte)((int)data[i + 1] ^ (int)PROTOCOL_XOR_CHAR);
                    depacketizedData[idx] = (byte)(unsignedToBytes(data[i + 1]) ^ (int)PROTOCOL_XOR_CHAR);
                    i++;
                }
                else
                {
                    depacketizedData[idx] = data[i];
                }
                idx++;
            }
            else // Reached end of data so store FCS bytes
            {
                if (idx2 < 2)
                {
                    if (data[i] == PROTOCOL_CTRL_ESC)
                    {
//                        fcs[idx2] = (byte)((int)data[i + 1] ^ (int)PROTOCOL_XOR_CHAR);
                        fcs[idx2] = (byte)(unsignedToBytes(data[i + 1]) ^ (int)PROTOCOL_XOR_CHAR);
                        i++;
                    }
                    else
                    {
                        fcs[idx2] = data[i];
                    }
                    idx2++;
                }
            }
        }

        if(IsFcsValid(fcs, depacketizedData)){
        	return depacketizedData;
        }
        else
        	return null;
    }
    public static int unsignedToBytes(byte b) {
        return b & 0xFF;
    }
    
    public static byte[] Packetize(byte[] data)
    {
        // look for characters that will require Control Escape byte
        //
        int numEscChars = 0;
        if(data == null)
        	return null;
        
        for (int i = 0; i < data.length; i++)  //skip first and last bytes because those are the header bytes
        {
            if (data[i] == PROTOCOL_FLAG_BYTE || data[i] == PROTOCOL_CTRL_ESC)
            {
                numEscChars++;
            }
        }

        byte[] fcs = GetFcs(data);

        for (int i = 0; i < fcs.length; i++)  //skip first and last bytes because those are the header bytes
        {
            if (fcs[i] == PROTOCOL_FLAG_BYTE || fcs[i] == PROTOCOL_CTRL_ESC)
            {
                numEscChars++;
            }
        }

        //stuff escape control characters
        //
        byte[] result = new byte[data.length + 2 + 2 + numEscChars];  //add 2bytes for head/tail, and 2 bytes for fcs, plus extra escape characters

        result[0] = (byte)PROTOCOL_FLAG_BYTE;
        result[result.length - 1] = (byte)PROTOCOL_FLAG_BYTE;

        int idx = 1;
        for (int i = 0; i < data.length; i++)
        {
            if (data[i] == PROTOCOL_FLAG_BYTE || data[i] == PROTOCOL_CTRL_ESC)
            {
                result[idx++] = (byte)PROTOCOL_CTRL_ESC;
                result[idx++] = (byte)(PROTOCOL_XOR_CHAR ^ data[i]);
            }
            else
            {
                result[idx++] = data[i];
            }
        }

        for (int i = 0; i < fcs.length; i++)
        {
            if (fcs[i] == PROTOCOL_FLAG_BYTE || fcs[i] == PROTOCOL_CTRL_ESC)
            {
                result[idx++] = (byte)PROTOCOL_CTRL_ESC;
                result[idx++] = (byte)(PROTOCOL_XOR_CHAR ^ fcs[i]);
            }
            else
            {
                result[idx++] = fcs[i];
            }
        }
        return result;
    }
}
