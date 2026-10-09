package com.example.lending.loan.servicing.recon.tree;

/**
 * Id arithmetic of the reconciliation partition tree. Node ids are numbered breadth first starting at 1:
 * the root is 1, its children 2 and 3, and so on.
 */
final class ReconTreeIds {

    private ReconTreeIds() {
    }

    static long maskOfId(long id) {
        long temp = id;
        int i = 0;
        while (temp != 1) {
            temp = temp >> 1;
            i++;
        }

        return (1L << i) - 1;
    }

    static long idOf(long mask, long index) {
        return mask + 1 + index;
    }
}
