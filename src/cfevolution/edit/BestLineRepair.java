package cfevolution.edit;

import cfevolution.data.track.CCLine;
import cfevolution.data.track.CCLineSegment;
import cfevolution.data.track.Track;
import cfevolution.data.track.TrackSegments;
import cfevolution.generator.ccline.CCLineTrackGeometry;
import cfevolution.generator.ccline.CCLineWindowRepair;

/**
 * Repair Best Line After Edit, without the interface (2026-11-16, moved out
 * of TrackWindow word for word). Editing one sector shifts every sector after
 * it (the line is stateful). The snapshot taken just before an edit applies
 * lets repair() refit a bounded window of following sectors back onto the
 * old line via CCLineWindowRepair.
 *
 * One per open track: call prepare() just before a best line edit is
 * applied and confirm() just after; repair() is offered while available().
 */
public final class BestLineRepair
{
    public static final String TITLE = "Repair Best Line";

    private final Track track;
    private double[] m_adLineSnapshot = null;
    private double[] m_adPendingSnapshot = null;
    private CCLineSegment m_EditedLineSegment = null;
    private CCLineSegment m_PendingLineSegment = null;

    public BestLineRepair(Track track)
    {
        this.track = track;
    }

    /** Captures the current (pre-edit) per-Seg line stamps. */
    public void prepare(CCLineSegment lineSegment)
    {
        TrackSegments segs = track.getTrackSegments();
        int n = segs.getMaxTrackSegIndex() + 1;
        m_adPendingSnapshot = new double[n];
        for (int i = 0; i < n; i++)
            m_adPendingSnapshot[i] = segs.getSegAt(i).getCCLine();
        m_PendingLineSegment = lineSegment;
    }

    /** Promotes the snapshot only when the segment actually changed, so
        re-applying identical values keeps the original pre-edit target.
        Returns whether a repair is now available. */
    public boolean confirm(boolean fChanged)
    {
        if (fChanged && m_adPendingSnapshot != null)
        {
            m_adLineSnapshot = m_adPendingSnapshot;
            m_EditedLineSegment = m_PendingLineSegment;
        }
        m_adPendingSnapshot = null;
        m_PendingLineSegment = null;
        return available();
    }

    public boolean available()
    {
        return m_adLineSnapshot != null && m_EditedLineSegment != null;
    }

    /** Forgets the edit (the whole line was replaced). */
    public void clear()
    {
        m_adLineSnapshot = null;
        m_EditedLineSegment = null;
    }

    /** What repair() produced: the repaired line to put in place (or null), and the message. */
    public static final class Result
    {
        public final CCLine repaired;
        public final Message message;

        Result(CCLine repaired, Message message)
        {
            this.repaired = repaired;
            this.message = message;
        }
    }

    /** The repaired line, not yet applied: apply it with TrackTools.replaceBestLine
        (which the caller follows with clear()). Null when nothing is to be repaired. */
    public Result repair()
    {
        if (!available())
            return null;
        CCLine line = track.getCCLine();
        int nIndex = -1;
        for (int i = 1; i <= line.size(); i++)
            if (line.getAt(i) == m_EditedLineSegment)
            {
                nIndex = i;
                break;
            }
        if (nIndex < 0)
        {
            clear();
            return new Result(null, Message.warning(TITLE, "The edited best line segment no longer exists."));
        }

        CCLineTrackGeometry geo = new CCLineTrackGeometry(track);
        double[] adTarget = new double[geo.segCount];
        for (int i = 0; i < geo.segCount; i++)
            adTarget[i] = (i < m_adLineSnapshot.length) ? m_adLineSnapshot[i] : 0.0;

        CCLineSegment replacement = new CCLineSegment(m_EditedLineSegment.getType());
        replacement.setTlu(m_EditedLineSegment.getTlu());
        for (int p = 0; p < 4; p++)
            replacement.setParam(p, m_EditedLineSegment.getParam(p));

        CCLine repaired = CCLineWindowRepair.replaceAndRepair(geo, line, nIndex,
            new CCLineSegment[] { replacement }, CCLineWindowRepair.DEFAULT_WINDOW_TLU, adTarget);
        if (repaired == null)
            return new Result(null, Message.warning(TITLE,
                "There is nothing to repair: the edited segment is the last one\n"
              + "in the best line."));
        return new Result(repaired, Message.info(TITLE,
            "The best line after your edit has been adjusted to follow its\n"
          + "previous path, so the rest of the lap is unaffected."));
    }
}
