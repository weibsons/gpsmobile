package mobi.stos.gpsmobile.dao;

import android.content.Context;

import mobi.stos.gpsmobile.bean.Position;
import mobi.stos.podataka_lib.repository.AbstractRepository;

public class PositionDao extends AbstractRepository<Position> {

    public PositionDao(Context context) {
        super(context, Position.class);
    }



}
