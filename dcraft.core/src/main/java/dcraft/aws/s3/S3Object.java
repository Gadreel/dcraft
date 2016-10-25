//  This software code is made available "AS IS" without warranties of any
//  kind.  You may copy, display, modify and redistribute the software
//  code either by itself or as incorporated into your code; provided that
//  you do not remove any proprietary notices.  Your use of this software
//  code is at your own risk and you waive any claim against Amazon
//  Digital Services, Inc. or its affiliates with respect to your use of
//  this software code. (c) 2006 Amazon Digital Services, Inc. or its
//  affiliates.

package dcraft.aws.s3;

import java.util.List;
import java.util.Map;

/**
 * A representation of a single object stored in S3.
 */
public class S3Object {

    public byte[] data;

    /**
     * A Map from String to List of Strings representing the object's metadata
     */
    public Map<String,List<String>> metadata;

    public S3Object(byte[] data, Map<String,List<String>> metadata) {
        this.data = data;
        this.metadata = metadata;
    }
}
