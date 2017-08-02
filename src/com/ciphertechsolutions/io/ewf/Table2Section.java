package com.ciphertechsolutions.io.ewf;

/**
 * It has the same specification as the table section.
 * Every segment file contains its own table2 section. It resides directly after the {@link com.ciphertechsolutions.io.ewf.TableSection}.
 * For EnCase6 the table2 section contains a mirror copy of the {@link com.ciphertechsolutions.io.ewf.TableSection}.
 * Probably intended for recovery purposes.
 *
 */
public class Table2Section extends TableSection {

    public Table2Section(long baseOffset) {
        super("table2", baseOffset);
    }

    public Table2Section(long currentOffset, TableSection toClone) {
        super("table2", currentOffset, toClone.getBaseOffset(), toClone.getSectionSize(), toClone.getArray(),
                toClone.getSecondaryAdler32(), toClone.getTableAdler32());
    }

}
