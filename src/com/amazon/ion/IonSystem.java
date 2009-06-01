// Copyright (c) 2007-2009 Amazon.com, Inc.  All rights reserved.

package com.amazon.ion;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.util.Date;
import java.util.Iterator;

/**
 * Entry point to all things Ion.
 * <p>
 * In general, instances returned from one system are not interchangable with
 * those returned by other systems.
 * The intended usage pattern is for an application to construct a single
 * <code>IonSystem</code> instance and use it throughout,
 * rather than constructing multiples and intermingling their use.
 * To create a copy of a value for use by a different system, use
 * {@link #clone(IonValue)}.
 * <p>
 * To create an {@code IonSystem},
 * see {@link com.amazon.ion.system.SystemFactory}.
 * <p>
 * Implementations of this interface must be safe for use by multiple threads.
 */
public interface IonSystem
    extends ValueFactory
{
    /**
     * Gets the default system symbol table.
     */
    public SymbolTable getSystemSymbolTable();


    /**
     * Gets a system symbol table for a specific version of Ion.
     *
     * @param ionVersionId must be of the form <code>"$ion_X_Y"</code>.
     * @return the requested system table.
     *
     * @throws UnsupportedIonVersionException if the requested version of
     * Ion is not supported by this implementation.
     */
    public SymbolTable getSystemSymbolTable(String ionVersionId)
        throws UnsupportedIonVersionException;


    /**
     * Gets the catalog used by this system.  Unless otherwise noted,
     * all objects derived from this system will use this catalog.
     */
    public IonCatalog getCatalog();


    /**
     * Sets the default catalog used by this system.
     *
     * @param catalog the new system catalog.
     * @throws NullPointerException if <code>catalog</code> is null.
     *
     * @deprecated  Catalog should be immutable.
     */
    @Deprecated
    public void setCatalog(IonCatalog catalog);


    /**
     * Creates a new local symbol table based on specific imported tables.
     * If the first imported table is a system table, then the local table will
     * use it appropriately. Otherwise, the local table will use this system's
     * {@linkplain #getCatalog() default catalog}.
     *
     * @param imports the set of shared symbol tables to import.
     * The first (and only the first) may be a system table.
     *
     * @return a new local symbol table.
     *
     * @throws IllegalArgumentException if any import is a local table,
     * or if any but the first is a system table.
     * @throws NullPointerException if any import is null.
     */
    public SymbolTable newLocalSymbolTable(SymbolTable... imports);


    /**
     * Creates a new shared symbol table containing a given set of symbols.
     * The table will contain symbols in the following order:
     * <ol>
     *   <li>
     *     If {@code version} is larger than 1, the prior version of the
     *     named table is retrieved from the catalog and all of its symbols
     *     are added.
     *   </li>
     *   <li>
     *     For each non-system table in {@code imports}, add all of its
     *     declared symbols.
     *   </li>
     *   <li>
     *     Add all of the symbols provided by {@code newSymbols}.
     *   </li>
     * </ol>
     * Any duplicate symbol texts or null strings are ignored.
     * <p>
     * This method is intended for use by utilities that are defining new
     * symbol tables for use by applications. The result will typically be
     * added to an {@link IonCatalog} which is responsible for persistence.
     * Shared symbol tables are serialized via
     * {@link SymbolTable#writeTo(IonWriter)} and materialized via
     * {@link #newSharedSymbolTable(IonReader)}.
     *
     * @param name the symbol table name, a non-empty string.
     * @param version at least one.
     * @param newSymbols provides symbol names; may be null.
     * @param imports other tables from which to import symbols.
     *
     * @return a new shared symbol table with the given name and version.
     *
     * @throws IonException if {@code version > 1} and the prior version does
     * not exist in this system's catalog.
     */
    public SymbolTable newSharedSymbolTable(String name,
                                            int version,
                                            Iterator<String> newSymbols,
                                            SymbolTable... imports);

    /**
     * Materializes a shared symbol table from its serialized form.
     *
     * @param reader must not be null.
     *
     * @return a new symbol table instance.
     */
    public SymbolTable newSharedSymbolTable(IonReader reader);


    /**
     * Creates a new empty datagram.
     *
     * @return a new datagram with no user values.
     */
    public IonDatagram newDatagram();


    /**
     * Creates a new datagram containing one value.  If the given value is
     * contained elsewhere, it is cloned before insertion.
     *
     * @param initialChild becomes the first and only (user) value in the
     * datagram.  The child's {@link IonValue#getSystem() system}
     * must be <em>this</em> system.
     * If {@code null}, then the returned datagram will have no
     * user values.
     *
     * @return a new datagram.
     *
     * @throws IllegalArgumentException
     *   if {@code initialChild} is an {@link IonDatagram}.
     */
    public IonDatagram newDatagram(IonValue initialChild);


    /**
     * Creates a new datagram, bootstrapped with imported symbol tables.
     * Generally an application will use this to aquire a datagram, then adds
     * values to it, then calls {@link IonDatagram#getBytes(byte[])}
     * (or similar) to extract binary data.
     *
     * @param imports the set of shared symbol tables to import.
     * The first (and only the first) may be a system table.
     *
     * @return a new datagram with no user values.
     *
     * @throws IllegalArgumentException if any import is a local table,
     * or if any but the first is a system table.
     *
     * @see #newLocalSymbolTable(SymbolTable...)
     */
    public IonDatagram newDatagram(SymbolTable... imports);

    /**
     * Constructs a new loader instance using the system catalog.
     */
    public IonLoader newLoader();

    /**
     * Constructs a new loader instance using the given catalog.
     * 
     * @see #newLoader()
     */
    public IonLoader newLoader(IonCatalog catalog);

    /**
     * Gets the default system loader.  Applications may replace this loader
     * with one configured appropriately, and then access it here.
     *
     * @return not <code>null</code>.
     *
     * @see #setLoader(IonLoader)
     */
    public IonLoader getLoader();


    /**
     * Sets the default system loader.
     *
     * @param loader The new system loader.
     * @throws NullPointerException if loader is null.
     * @throws IllegalArgumentException if <code>loader.getSystem()</code> is
     * not this system.
     *
     * @deprecated Default loader should be immutable.
     */
    @Deprecated
    public void setLoader(IonLoader loader);


    /**
     * Creates an iterator over a stream of Ion text data.
     * Values returned by the iterator have no container.
     * <p>
     * The iterator will automatically consume Ion system IDs and local symbol
     * tables; they will not be returned by the iterator.
     * <p>
     * This method is suitable for use over unbounded streams with a reasonable
     * schema.
     *
     * @param ionText a stream of Ion text data.  The caller is responsible for
     * closing the Reader after iteration is complete.
     *
     * @return a new iterator instance.
     *
     * @throws NullPointerException if <code>ionText</code> is null.
     */
    public Iterator<IonValue> iterate(Reader ionText);


    /**
     * Creates an iterator over a string containing Ion text data.
     * Values returned by the iterator have no container.
     * <p>
     * The iterator will automatically consume Ion system IDs and local symbol
     * tables; they will not be returned by the iterator.
     *
     * @param ionText must not be null.
     *
     * @return a new iterator instance.
     *
     * @throws NullPointerException if <code>ionText</code> is null.
     */
    public Iterator<IonValue> iterate(String ionText);


    /**
     * Creates an iterator over Ion data.
     * Values returned by the iterator have no container.
     * <p>
     * The iterator will automatically consume Ion system IDs and local symbol
     * tables; they will not be returned by the iterator.
     *
     * @param ionData may be either Ion binary data or (UTF-8) Ion text.
     * <em>This method assumes ownership of the array</em> and may modify it at
     * will.
     *
     * @return a new iterator instance.
     *
     * @throws NullPointerException if <code>ionData</code> is null.
     */
    public Iterator<IonValue> iterate(byte[] ionData);


    /**
     * Extracts a single value from Ion text data.
     *
     * @param ionText must not be null.
     *
     * @return the first (and only) user value in the data.
     *
     * @throws NullPointerException if <code>ionText</code> is null.
     * @throws IonException if the data does not contain exactly one user
     * value.
     */
    public IonValue singleValue(String ionText);


    /**
     * Extracts a single value from Ion text or binary data.
     *
     * @param ionData may be either Ion binary data or (UTF-8) Ion text.
     * <em>This method assumes ownership of the array</em> and may modify it at
     * will.
     *
     * @return the first (and only) user value in the data.
     *
     * @throws IonException if the data does not contain exactly one user
     * value.
     */
    public IonValue singleValue(byte[] ionData);


    //-------------------------------------------------------------------------
    // IonReader creation

//  public IonReader newReader(Reader ionText); // TODO add newReader(Reader)


    /**
     * Creates an new {@link IonReader} instance over Ion text data.
     * <p>
     * The text is parsed incrementally by the reader, so any syntax errors
     * will not be detected here.
     *
     * @param ionText must not be null.
     */
    public IonReader newReader(String ionText);

    /**
     * Creates an new {@link IonReader} instance over a block of Ion data,
     * detecting whether it's text or binary data.
     *
     * @param ionData may be either Ion binary data, or UTF-8 Ion text.
     * The reader retains a reference to the array, so its data must not be
     * modified while the reader is active.
     */
    public IonReader newReader(byte[] ionData);

    /**
     * Creates an new {@link IonReader} instance over a block of Ion data,
     * detecting whether it's text or binary data.
     *
     * @param ionData is used only within the range of bytes starting at
     * {@code offset} for {@code len} bytes.
     * The data in that range may be either Ion binary data, or UTF-8 Ion text.
     * The reader retains a reference to the array, so its data must not be
     * modified while the reader is active.
     * @param offset must be non-negative and less than {@code ionData.length}.
     * @param len must be non-negative and {@code offset+len} must not exceed
     * {@code ionData.length}.
     */
    public IonReader newReader(byte[] ionData, int offset, int len);

    /**
     * Creates a new {@link IonReader} instance over a stream of Ion data,
     * detecting whether it's text or binary data.
     * <p>
     * <b>NOTE:</b> The current implementation of this method reads the entire
     * contents of the input stream into memory.
     *
     * @param ionData must not be null.
     *
     * @return a new reader instance.
     */
    public IonReader newReader(InputStream ionData);

    /**
     * Creates an new {@link IonReader} instance over an {@link IonValue} data
     * model. Typically this is used to iterate over a collection, such as an
     * {@link IonStruct}.
     *
     * @param value must not be null.
     */
    public IonReader newReader(IonValue value);


    //-------------------------------------------------------------------------
    // IonWriter creation

    /**
     * Creates a new writer that will add {@link IonValue}s to the given
     * container.
     *
     * @param container a container that will receive new children from the
     * the returned writer.
     * Must not be null.
     *
     * @return a new {@link IonWriter} instance; not {@code null}.
     */
    public IonWriter newWriter(IonContainer container);

    /**
     * Creates a new writer that will write UTF-8 text to the given output
     * stream.
     *
     * @param out the stream that will receive UTF-8 Ion text data.
     * Must not be null.
     *
     * @return a new {@link IonWriter} instance; not {@code null}.
     */
    public IonWriter newTextWriter(OutputStream out);

    /**
     * Creates a new writer that will write text to the given output
     * stream.
     *
     * @param out the stream that will receive Ion text data.
     * Must not be null.
     *
     * @return a new {@link IonWriter} instance; not {@code null}.
     */
    public IonWriter newTextWriter(Appendable out);

    /**
     * Creates a new writer that will write UTF-8 text to the given output
     * stream, using the given shared symbol tables as imports.
     * <p>
     * The output stream will start with an Ion Version Marker and a
     * local symbol table that uses the given {@code imports}.
     *
     * @param out the stream that will receive UTF-8 Ion text data.
     * Must not be null.
     * @param imports a sequence of shared symbol tables
     *
     * @return a new {@link IonWriter} instance; not {@code null}.
     *
     * @throws IOException if its thrown by the output stream.
     */
    public IonWriter newTextWriter(OutputStream out, SymbolTable... imports)
        throws IOException;

    /**
     * Creates a new writer that will write text to the given output
     * stream, using the given shared symbol tables as imports.
     * <p>
     * The output stream will start with an Ion Version Marker and a
     * local symbol table that uses the given {@code imports}.
     *
     * @param out the stream that will receive Ion text data.
     * Must not be null.
     * @param imports a sequence of shared symbol tables
     *
     * @return a new {@link IonWriter} instance; not {@code null}.
     *
     * @throws IOException if its thrown by the output stream.
     */
    public IonWriter newTextWriter(Appendable out, SymbolTable... imports)
        throws IOException;

    /**
     * Creates a new writer that will encode binary Ion data.
     *
     * @return a new {@link IonBinaryWriter} instance; not {@code null}.
     */
    public IonBinaryWriter newBinaryWriter();

    /**
     * Creates a new writer that will encode binary Ion data,
     * using the given shared symbol tables as imports.
     * <p>
     * The output stream will be start with an Ion Version Marker and a
     * local symbol table that uses the given {@code imports}.
     *
     * @param imports a sequence of shared symbol tables
     *
     * @return a new {@link IonBinaryWriter} instance; not {@code null}.
     */
    public IonBinaryWriter newBinaryWriter(SymbolTable... imports);


    //-------------------------------------------------------------------------
    // DOM creation

    /**
     * Constructs a new <code>null.blob</code> instance.
     * @deprecated Use {@link #newNullBlob()} instead.
     */
    @Deprecated
    public IonBlob newBlob();


    /**
     * Constructs a new <code>null.bool</code> instance.
     * @deprecated Use {@link #newNullBool()} instead
     */
    @Deprecated
    public IonBool newBool();


    /**
     * Constructs a new <code>null.clob</code> instance.
     * @deprecated Use {@link #newNullClob()} instead
     */
    @Deprecated
    public IonClob newClob();


    /**
     * Constructs a new <code>null.decimal</code> instance.
     * @deprecated Use {@link #newNullDecimal()} instead
     */
    @Deprecated
    public IonDecimal newDecimal();


    /**
     * Constructs a new <code>null.float</code> instance.
     * @deprecated Use {@link #newNullFloat()} instead
     */
    @Deprecated
    public IonFloat newFloat();


    /**
     * Constructs a new <code>null.int</code> instance.
     * @deprecated Use {@link #newNullInt()} instead
     */
    @Deprecated
    public IonInt newInt();


    /**
     * Constructs a new <code>null.list</code> instance.
     * @deprecated Use {@link #newNullList()} instead
     */
    @Deprecated
    public IonList newList();


    /**
     * Constructs a new <code>null.sexp</code> instance.
     * @deprecated Use {@link #newNullSexp()} instead
     */
    @Deprecated
    public IonSexp newSexp();


    /**
     * Constructs a new <code>null.string</code> instance.
     * @deprecated Use {@link #newNullString()} instead
     */
    @Deprecated
    public IonString newString();


    /**
     * Constructs a new <code>null.struct</code> instance.
     * @deprecated Use {@link #newNullStruct()} instead
     */
    @Deprecated
    public IonStruct newStruct();


    /**
     * Constructs a new <code>null.symbol</code> instance.
     * @deprecated Use {@link #newNullSymbol()} instead
     */
    @Deprecated
    public IonSymbol newSymbol();


    /**
     * Constructs a new <code>null.timestamp</code> instance.
     * @deprecated Use {@link #newNullTimestamp()} instead
     */
    @Deprecated
    public IonTimestamp newTimestamp();


    /**
     * Constructs a new UTC <code>timestamp</code> initialized to represent
     * the specified number of milliseconds since the standard base time known
     * as "the epoch", namely 1970-01-01T00:00:00Z.
     *
     * @param millis the milliseconds since 1970-01-01T00:00:00Z.
     */
    public IonTimestamp newUtcTimestampFromMillis(long millis);


    /**
     * Constructs a new UTC <code>timestamp</code> instance initialized so that
     * it represents the given time.  As per {@link Date} class, this will have
     * millisecond precision.
     * <p>
     * This is equivalent to
     * <code>{@linkplain #newUtcTimestampFromMillis newUtcTimestampFromMillis}(utcDate.getTime())</code>.
     *
     * @param utcDate the time of the new instance;
     * may be <code>null</code> to make <code>null.timestamp</code>.
     */
    public IonTimestamp newUtcTimestamp(Date utcDate);


    /**
     * Constructs a new UTC <code>timestamp</code> instance initialized so that
     * it represents the time at which it was allocated, measured to the nearest
     * millisecond.
     */
    public IonTimestamp newCurrentUtcTimestamp();


    /**
     * Creates a deep copy of an Ion value.  This method can properly clone
     * {@link IonDatagram}s.
     * <p>
     * The given value can be in the context of any {@code IonSystem} instance,
     * and the result will be in the context of this system. This allows you to
     * shift data from one system instance to another.
     *
     * @param value the value to copy.
     * @return a deep copy of value, with no container.
     * @throws NullPointerException if <code>value</code> is null.
     * @throws IonException if there's a problem creating the clone.
     *
     * @see IonValue#clone()
     */
    public <T extends IonValue> T clone(T value)
        throws IonException;
}
