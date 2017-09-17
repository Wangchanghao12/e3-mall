package cn.e3mall.service;

import cn.e3mall.common.pojo.E3Result;
import cn.e3mall.common.pojo.EasyUIDataGridResult;
import cn.e3mall.common.utils.IDUtils;
import cn.e3mall.mapper.TbItemDescMapper;
import cn.e3mall.mapper.TbItemMapper;
import cn.e3mall.pojo.TbItem;
import cn.e3mall.pojo.TbItemDesc;
import cn.e3mall.pojo.TbItemExample;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.stereotype.Service;

import javax.jms.*;
import java.util.Date;
import java.util.List;

@Service
public class ItemServiceImpl implements ItemService {

    @Autowired
    private TbItemMapper tbItemMapper;
    @Autowired
    private TbItemDescMapper tbItemDescMapper;
    @Autowired
    private JmsTemplate jmsTemplate;
    @Autowired
    private Destination topicDestination;

    public TbItem getItemById(Long id){
        TbItem tbItem = this.tbItemMapper.selectByPrimaryKey(id);
        return tbItem;
    }

    @Override
    public EasyUIDataGridResult getItemList(int page, int rows) {
        //设置分页信息
        PageHelper.startPage(page,rows);
        //执行查询
        TbItemExample tbItemExample = new TbItemExample();
        List<TbItem> tbItems = this.tbItemMapper.selectByExample(tbItemExample);
        //取分页信息
        PageInfo<TbItem> pageInfo = new PageInfo<>(tbItems);

        //创建返回结果对象
        EasyUIDataGridResult easyUIDataGridResult = new EasyUIDataGridResult();
        easyUIDataGridResult.setTotal(pageInfo.getTotal());
        easyUIDataGridResult.setRows(tbItems);
        return easyUIDataGridResult;
    }

    @Override
    public E3Result addItem(TbItem item, String description) {
        //将生成的商品id final方便放入广播消息中(内部类)
        final long id = IDUtils.genItemId();
        //补全item数据
        item.setId(id);
        //1-正常，2-下架，3-删除
        item.setStatus((byte) 1);
        item.setCreated(new Date());
        item.setUpdated(new Date());
        this.tbItemMapper.insert(item);
        //补全属性
        TbItemDesc tbItemDesc = new TbItemDesc();
        tbItemDesc.setItemId(id);
        tbItemDesc.setCreated(new Date());
        tbItemDesc.setItemDesc(description);
        tbItemDesc.setUpdated(new Date());
        this.tbItemDescMapper.insert(tbItemDesc);
        //广播商品已添加的消息
        jmsTemplate.send(topicDestination, new MessageCreator(){
            @Override
            public Message createMessage(Session session) throws JMSException {
                TextMessage textMessage = session.createTextMessage(id + "");
                //TextMessage textMessage = session.createTextMessage(itemId + "");
                return textMessage;
            }
        });
        //返回成功
       return E3Result.ok();
    }

    @Override
    public E3Result deleteItem(String ids) {
//      TbItem item = this.tbItemMapper.selectByPrimaryKey(id);
//      item.setStatus((byte)3);
//      this.tbItemMapper.updateByPrimaryKey(item);
        String[] splits = ids.split(",");
        for (String st : splits) {
            Long id = Long.parseLong(st);
            this.tbItemMapper.deleteByPrimaryKey(id);
          this.tbItemDescMapper.deleteByPrimaryKey(id);
        }
        return E3Result.ok();
    }

    @Override
    public E3Result instockItem(String ids) {
        String[] splits = ids.split(",");
        for (String st : splits) {
            Long id = Long.parseLong(st);
            TbItem item = this.tbItemMapper.selectByPrimaryKey(id);
            //1-正常，2-下架，3-删除
            item.setStatus((byte)2);
            this.tbItemMapper.updateByPrimaryKey(item);
        }
        return E3Result.ok();
    }

    @Override
    public E3Result reshelfItem(String ids) {
        String[] splits = ids.split(",");
        for (String st : splits) {
            Long id = Long.parseLong(st);
            TbItem item = this.tbItemMapper.selectByPrimaryKey(id);
            //1-正常，2-下架，3-删除
            item.setStatus((byte)1);
            this.tbItemMapper.updateByPrimaryKey(item);
        }
        return E3Result.ok();
    }

    @Override
    public TbItemDesc getItemDescById(String id) {
        return  this.tbItemDescMapper.selectByPrimaryKey(Long.parseLong(id));
    }

    @Override
    public E3Result updateItem(TbItem item ,String desc) {
        Long id = item.getId();
        //修改描述
        TbItemDesc itemDesc = this.tbItemDescMapper.selectByPrimaryKey(id);
        itemDesc.setUpdated(new Date());
        itemDesc.setItemDesc(desc);
        //修改产品信息
        this.tbItemMapper.updateByPrimaryKey(item);
        return E3Result.ok();
    }
}
