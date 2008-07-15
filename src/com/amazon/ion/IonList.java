/* Copyright (c) 2007-2008 Amazon.com, Inc.  All rights reserved. */

package com.amazon.ion;


/**
 * An Ion <code>list</code> value.
 */
public interface IonList
    extends IonValue, IonSequence
{
    public IonList clone();
}
